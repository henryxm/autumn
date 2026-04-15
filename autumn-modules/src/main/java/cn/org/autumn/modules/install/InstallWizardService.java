package cn.org.autumn.modules.install;

import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.install.InstallRestartCoordinator;
import cn.org.autumn.modules.install.dto.InstallConnectionForm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "autumn.install", name = "mode", havingValue = "true")
public class InstallWizardService {

    private static final Logger log = LoggerFactory.getLogger(InstallWizardService.class);

    private static final int META_TABLE_DISPLAY_LIMIT = 500;

    @Value("${" + InstallConstants.CONFIG_PATH + ":" + InstallConstants.DEFAULT_CONFIG_FILE + "}")
    private String configPathProperty;

    @Autowired
    private InstallRestartCoordinator restartCoordinator;

    public File resolvedConfigFile() {
        File f = new File(configPathProperty);
        if (!f.isAbsolute()) {
            f = new File(System.getProperty("user.dir", "."), configPathProperty).getAbsoluteFile();
        }
        return f;
    }

    public void setAgreed(HttpSession session) {
        session.setAttribute(InstallConstants.SESSION_AGREED, Boolean.TRUE);
    }

    public boolean isAgreed(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(InstallConstants.SESSION_AGREED));
    }

    public void saveForm(HttpSession session, InstallConnectionForm form) {
        session.setAttribute(InstallConstants.SESSION_FORM, form);
        session.removeAttribute(InstallConstants.SESSION_CHECKS_OK);
    }

    public InstallConnectionForm getForm(HttpSession session) {
        return (InstallConnectionForm) session.getAttribute(InstallConstants.SESSION_FORM);
    }

    public Map<String, Object> testAndDescribePrivileges(HttpSession session) throws Exception {
        requireAgreed(session);
        InstallConnectionForm form = getForm(session);
        if (form == null) {
            throw new IllegalStateException("请先填写数据库连接信息");
        }
        InstallJdbcHelper.ResolvedJdbc r = InstallJdbcHelper.resolve(form);
        Class.forName(r.getDriverClassName());
        try (Connection conn = DriverManager.getConnection(r.getJdbcUrl(), form.getUsername(), form.getPassword())) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            String product = conn.getMetaData().getDatabaseProductName();
            String version = conn.getMetaData().getDatabaseProductVersion();
            out.put("product", product);
            out.put("version", version);
            out.put("driver", r.getDriverClassName());
            String masked = InstallJdbcUrlMask.mask(r.getJdbcUrl());
            out.put("jdbcMasked", masked);
            privilegeProbe(conn, r.getDatabaseType());
            session.setAttribute(InstallConstants.SESSION_CHECKS_OK, Boolean.TRUE);

            List<String> summaryLines = new ArrayList<>();
            summaryLines.add("已成功连接数据库。");
            summaryLines.add("产品：" + product + "。");
            if (r.getDatabaseType() == DatabaseType.H2) {
                summaryLines.add("H2 兼容模式：" + InstallJdbcHelper.H2CompatibilityMode.fromForm(form).name() + "。");
            }
            summaryLines.add("已测试建表与删表，账号具备安装所需权限。");
            out.put("summaryLines", summaryLines);

            List<Map<String, Object>> sections = new ArrayList<>();
            Map<String, Object> secDb = new LinkedHashMap<>();
            secDb.put("title", "数据库信息");
            List<String> dbLines = new ArrayList<>();
            dbLines.add("产品名称：" + product);
            dbLines.add("版本信息：" + (version != null ? version : "—"));
            dbLines.add("JDBC 驱动类：" + r.getDriverClassName());
            secDb.put("lines", dbLines);
            sections.add(secDb);

            Map<String, Object> secConn = new LinkedHashMap<>();
            secConn.put("title", "连接概要");
            List<String> connLines = new ArrayList<>();
            if (StringUtils.isNotBlank(form.getUsername())) {
                connLines.add("登录用户：" + form.getUsername().trim());
            }
            if (r.getDatabaseType() == DatabaseType.H2) {
                connLines.add("H2 兼容模式：" + InstallJdbcHelper.H2CompatibilityMode.fromForm(form).name());
            }
            connLines.add("连接地址（已脱敏，不含密码）：" + masked);
            secConn.put("lines", connLines);
            sections.add(secConn);

            Map<String, Object> secPriv = new LinkedHashMap<>();
            secPriv.put("title", "权限检测");
            secPriv.put("lines", Collections.singletonList("已在当前库中创建并删除临时测试表，结果正常。"));
            sections.add(secPriv);
            out.put("sections", sections);

            return out;
        }
    }

    public Map<String, Object> readMetadata(HttpSession session) throws Exception {
        requireAgreed(session);
        if (!Boolean.TRUE.equals(session.getAttribute(InstallConstants.SESSION_CHECKS_OK))) {
            throw new IllegalStateException("请先通过连接与权限检测");
        }
        InstallConnectionForm form = getForm(session);
        InstallJdbcHelper.ResolvedJdbc r = InstallJdbcHelper.resolve(form);
        Class.forName(r.getDriverClassName());
        try (Connection conn = DriverManager.getConnection(r.getJdbcUrl(), form.getUsername(), form.getPassword())) {
            DatabaseMetaData md = conn.getMetaData();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("product", md.getDatabaseProductName());
            out.put("version", md.getDatabaseProductVersion());
            out.put("catalog", conn.getCatalog());
            out.put("schema", conn.getSchema());
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = md.getTables(conn.getCatalog(), getSchemaForMeta(conn, md), "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String schem = rs.getString("TABLE_SCHEM");
                    String tname = rs.getString("TABLE_NAME");
                    String prefix = schem != null && !schem.isEmpty() ? schem + "." : "";
                    tables.add(prefix + tname);
                }
            }
            int total = tables.size();
            boolean truncated = total > META_TABLE_DISPLAY_LIMIT;
            List<String> displayTables = truncated ? new ArrayList<>(tables.subList(0, META_TABLE_DISPLAY_LIMIT)) : tables;

            List<String> introLines = new ArrayList<>();
            introLines.add("已读取当前连接下的库信息。");
            introLines.add("数据库产品：" + md.getDatabaseProductName() + "。");
            String cat = conn.getCatalog();
            String sc = conn.getSchema();
            if (StringUtils.isNotBlank(cat) || StringUtils.isNotBlank(sc)) {
                introLines.add("当前目录/模式：" + StringUtils.defaultString(cat, "—") + " / " + StringUtils.defaultString(sc, "—") + "。");
            }
            introLines.add("共检测到 " + total + " 张数据表。");
            if (truncated) {
                introLines.add("下列列表仅展示前 " + META_TABLE_DISPLAY_LIMIT + " 个名称，完整数量以上述统计为准。");
            }
            if (r.getDatabaseType().supportsAnnotationTableSync()) {
                introLines.add("安装完成后，系统将按配置自动维护与业务模型对应的表结构。");
            } else {
                introLines.add("当前数据库类型可能不走注解自动建表，请与运维确认表结构维护方式。");
            }

            out.put("introLines", introLines);
            out.put("tables", displayTables);
            out.put("tableCount", total);
            out.put("tablesTruncated", truncated);
            out.put("annotationTableSync", r.getDatabaseType().supportsAnnotationTableSync());
            return out;
        }
    }

    private static String getSchemaForMeta(Connection conn, DatabaseMetaData md) throws java.sql.SQLException {
        String s = conn.getSchema();
        if (StringUtils.isNotBlank(s)) {
            return s;
        }
        if (md.getDatabaseProductName() != null && md.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql")) {
            return conn.getCatalog();
        }
        return null;
    }

    /**
     * 在目标库中建表、删表以验证 DDL 权限。
     */
    private static void privilegeProbe(Connection conn, DatabaseType type) throws Exception {
        String name = "autumn_inst_probe_" + Math.abs(System.nanoTime());
        try (Statement st = conn.createStatement()) {
            switch (type) {
                case POSTGRESQL:
                case KINGBASE:
                    st.executeUpdate("CREATE TABLE \"" + name + "\" (x INT)");
                    st.executeUpdate("DROP TABLE \"" + name + "\"");
                    break;
                case ORACLE:
                case OCEANBASE_ORACLE:
                case DAMENG:
                    st.executeUpdate("CREATE TABLE " + name + " (x NUMBER(1))");
                    st.executeUpdate("DROP TABLE " + name);
                    break;
                case SQLSERVER:
                    st.executeUpdate("CREATE TABLE [" + name + "] (x INT)");
                    st.executeUpdate("DROP TABLE [" + name + "]");
                    break;
                case DB2:
                    st.executeUpdate("CREATE TABLE " + name + " (x INT)");
                    st.executeUpdate("DROP TABLE " + name);
                    break;
                case FIREBIRD:
                    st.executeUpdate("CREATE TABLE " + name + " (x INTEGER)");
                    st.executeUpdate("DROP TABLE " + name);
                    break;
                default:
                    st.executeUpdate("CREATE TABLE " + name + " (x INT)");
                    st.executeUpdate("DROP TABLE " + name);
            }
        }
    }

    /**
     * 写入配置文件并由主线程关闭上下文后二次启动；不在当前进程对业务库执行建表（避免与占位 H2 并存时状态混乱）。
     */
    public Map<String, Object> finalizeInstall(HttpSession session) throws Exception {
        requireAgreed(session);
        if (!Boolean.TRUE.equals(session.getAttribute(InstallConstants.SESSION_CHECKS_OK))) {
            throw new IllegalStateException("请先通过检测步骤");
        }
        InstallConnectionForm form = getForm(session);
        InstallJdbcHelper.ResolvedJdbc r = InstallJdbcHelper.resolve(form);
        if (form.isEnableRedis()) {
            if (StringUtils.isBlank(form.getRedisHost())) {
                throw new IllegalStateException("已勾选启用 Redis，请填写 Redis 主机地址");
            }
            if (StringUtils.isBlank(form.getRedisPort())) {
                throw new IllegalStateException("已勾选启用 Redis，请填写 Redis 端口");
            }
        }

        File out = resolvedConfigFile();
        File parent = out.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        String yaml = buildYaml(r, form);
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(out.toPath()), StandardCharsets.UTF_8)) {
            w.write(yaml);
        }
        log.info("已写入安装配置: {}", out.getAbsolutePath());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("configPath", out.getAbsolutePath());
        resp.put("message", "配置已保存。系统将自动重启，重启后会连接您刚才填写的数据库并创建数据表、写入初始内容，请稍候再访问网站。");

        Executors.newSingleThreadScheduledExecutor(task -> {
            Thread t = new Thread(task, "autumn-install-restart-signal");
            t.setDaemon(true);
            return t;
        }).schedule(() -> {
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            restartCoordinator.signalRestart();
        }, 0, TimeUnit.MILLISECONDS);

        return resp;
    }

    private String buildYaml(InstallJdbcHelper.ResolvedJdbc r, InstallConnectionForm form) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("autumn:\n");
        sb.append("  installed: true\n");
        DatabaseType dt = r.getDatabaseType();
        if (dt != null && dt != DatabaseType.OTHER) {
            sb.append("  database: ").append(dt.name().toLowerCase(Locale.ROOT)).append("\n");
        }
        sb.append("  table:\n");
        sb.append("    init: true\n");
        sb.append("  redis:\n");
        sb.append("    open: ").append(form.isEnableRedis()).append("\n");
        sb.append("  shiro:\n");
        sb.append("    redis: ").append(form.isEnableRedis() && form.isEnableShiroRedis()).append("\n");
        sb.append("spring:\n");
        sb.append("  datasource:\n");
        sb.append("    driverClassName: ").append(quoteYamlScalar(r.getDriverClassName())).append("\n");
        sb.append("    druid:\n");
        sb.append("      first:\n");
        sb.append("        url: ").append(quoteYamlScalar(r.getJdbcUrl())).append("\n");
        sb.append("        username: ").append(quoteYamlScalar(form.getUsername())).append("\n");
        sb.append("        password: ").append(quoteYamlScalar(form.getPassword() == null ? "" : form.getPassword())).append("\n");
        sb.append("      second:\n");
        sb.append("        url: ").append(quoteYamlScalar(r.getJdbcUrl())).append("\n");
        sb.append("        username: ").append(quoteYamlScalar(form.getUsername())).append("\n");
        sb.append("        password: ").append(quoteYamlScalar(form.getPassword() == null ? "" : form.getPassword())).append("\n");
        if (form.isEnableRedis()) {
            sb.append("  redis:\n");
            sb.append("    host: ").append(quoteYamlScalar(form.getRedisHost().trim())).append("\n");
            sb.append("    port: ").append(parseRedisPort(form.getRedisPort())).append("\n");
            sb.append("    database: 0\n");
            sb.append("    password: ").append(quoteYamlScalar(form.getRedisPassword() == null ? "" : form.getRedisPassword())).append("\n");
            sb.append("    timeout: 6000ms\n");
        }
        return sb.toString();
    }

    private static int parseRedisPort(String raw) {
        if (StringUtils.isBlank(raw)) {
            return 6379;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Redis 端口无效: " + raw);
        }
    }

    private static String quoteYamlScalar(String s) {
        if (s == null) {
            return "\"\"";
        }
        String v = s;
        if (v.contains("\n") || v.contains("\"") || v.contains("'") || v.contains(":") || v.contains("#")
                || v.contains("\\") || v.trim().length() != v.length()) {
            return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return v;
    }

    private static void requireAgreed(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute(InstallConstants.SESSION_AGREED))) {
            throw new IllegalStateException("请先阅读并同意协议");
        }
    }

}
