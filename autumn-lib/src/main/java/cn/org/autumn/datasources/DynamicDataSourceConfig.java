package cn.org.autumn.datasources;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 配置多数据源
 */
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class DynamicDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.druid.first")
    public DataSource firstDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * first、second 若指向同一物理库，只保留一个 Druid 物理池并复用为路由中的 second，避免：
     * <ul>
     *     <li>嵌入式 Derby 双池并发 boot（ERROR XSDB6）</li>
     *     <li>SQLite 等同文件双池初始化长时间阻塞</li>
     *     <li>其它场景下无必要地双倍占用连接与池线程</li>
     * </ul>
     * 判断规则见 {@link #samePhysicalDatabase(String, String)}。
     * <p>
     * 不在此单独注册 {@code secondDataSource} Bean，避免其依赖 {@code firstDataSource} 与路由 {@code dataSource}
     * 之间形成 Spring 无法拆解的循环依赖。
     */
    @Bean
    @Primary
    public DynamicDataSource dataSource(Environment environment, @Qualifier("firstDataSource") DataSource firstDataSource) {
        String u1 = environment.getProperty("spring.datasource.druid.first.url", "");
        String u2 = environment.getProperty("spring.datasource.druid.second.url", "");
        // 未配置 second.url 时视为与 first 同库（共池），避免误建空 URL 的第二池；若业务上刻意留空且期望异构第二源，须显式配置 second.url。
        if (u2 == null || u2.trim().isEmpty()) {
            u2 = u1;
        }
        DataSource second;
        if (samePhysicalDatabase(u1, u2)) {
            second = firstDataSource;
        } else {
            DruidDataSource ds = DruidDataSourceBuilder.create().build();
            Binder.get(environment).bind("spring.datasource.druid.second", Bindable.ofInstance(ds));
            second = ds;
        }
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceNames.FIRST, firstDataSource);
        targetDataSources.put(DataSourceNames.SECOND, second);
        return new DynamicDataSource(firstDataSource, targetDataSources);
    }

    /**
     * 两条 JDBC URL 是否指向同一物理数据库（忽略查询串、分号后的会话参数等常见「非库身份」差异）。
     * 网络型 URL 的 authority 中 {@code localhost} 与 {@code 127.0.0.1} 视为同一地址。
     * 无法按已知方言解析时，回退为整串 URL 的忽略大小写比较（配置完全相同则共池）。
     */
    static boolean samePhysicalDatabase(String firstUrl, String secondUrl) {
        if (firstUrl == null || secondUrl == null) {
            return false;
        }
        String a = firstUrl.trim();
        String b = secondUrl.trim();
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        String ka = physicalJdbcIdentity(a);
        String kb = physicalJdbcIdentity(b);
        if (ka != null && kb != null) {
            return ka.equals(kb);
        }
        if (ka == null && kb == null) {
            return a.equalsIgnoreCase(b);
        }
        return false;
    }

    /**
     * 稳定身份串：同库不同写法（仅参数不同）应对齐到同一 key；无法识别时返回 {@code null}。
     */
    private static String physicalJdbcIdentity(String jdbcUrl) {
        String u = jdbcUrl.trim().toLowerCase(Locale.ROOT);
        if (u.startsWith("jdbc:derby:")) {
            return "derby:" + derbyPath(u);
        }
        if (u.startsWith("jdbc:sqlite:")) {
            return "sqlite:" + normalizeSqliteFilePath(sqliteLocation(u));
        }
        if (u.startsWith("jdbc:h2:")) {
            return "h2:" + subjdbcMainSegment(u, "jdbc:h2:");
        }
        if (u.startsWith("jdbc:hsqldb:")) {
            return "hsqldb:" + subjdbcMainSegment(u, "jdbc:hsqldb:");
        }
        if (u.startsWith("jdbc:mysql:") || u.startsWith("jdbc:mariadb:")) {
            return "mysql:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:postgresql:") || u.startsWith("jdbc:pgsql:")) {
            return "postgresql:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:sqlserver:") || u.startsWith("jdbc:microsoft:sqlserver:")) {
            return "sqlserver:" + sqlServerPhysicalKey(u);
        }
        if (u.startsWith("jdbc:oracle:thin:")) {
            return "oracle-thin:" + oracleThinPhysicalKey(u);
        }
        if (u.startsWith("jdbc:kingbase8:") || u.startsWith("jdbc:kingbase86:")) {
            return "kingbase:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:dm:") || u.startsWith("jdbc:dm8:")) {
            return "dm:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:oceanbase:")) {
            return "oceanbase:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:tidb:")) {
            return "tidb:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:db2:")) {
            return "db2:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:informix-sqli:") || u.startsWith("jdbc:informix:")) {
            return "informix:" + authoritySlashDatabase(u);
        }
        if (u.startsWith("jdbc:firebirdsql:") || u.startsWith("jdbc:firebird:")) {
            return "firebird:" + authoritySlashDatabase(u);
        }
        return null;
    }

    private static String derbyPath(String jdbcUrlLowercase) {
        String rest = jdbcUrlLowercase.substring("jdbc:derby:".length());
        int semi = rest.indexOf(';');
        return (semi >= 0 ? rest.substring(0, semi) : rest).trim();
    }

    private static String sqliteLocation(String jdbcUrlLowercase) {
        String rest = jdbcUrlLowercase.substring("jdbc:sqlite:".length());
        int q = rest.indexOf('?');
        if (q >= 0) {
            rest = rest.substring(0, q);
        }
        return rest.trim();
    }

    /**
     * 文件路径等价：{@code ./a/b} 与 {@code a/b} 视为同一库；反斜杠统一为 {@code /}。
     * 内存库、{@code file:} 命名内存等子协议不改动。
     */
    private static String normalizeSqliteFilePath(String locationLowercase) {
        if (locationLowercase == null || locationLowercase.isEmpty()) {
            return "";
        }
        String p = locationLowercase.replace('\\', '/').trim();
        if (":memory:".equals(p) || p.startsWith("file:") || p.startsWith(":resource:")) {
            return p;
        }
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        return p;
    }

    /** H2、HSQLDB 等：主路径段，去掉 {@code ;} / {@code ?} 后的属性。 */
    private static String subjdbcMainSegment(String jdbcUrlLowercase, String prefix) {
        String rest = jdbcUrlLowercase.substring(prefix.length());
        int cut = firstSemicolonOrQuestion(rest);
        String seg = (cut >= 0 ? rest.substring(0, cut) : rest).trim();
        if (seg.contains("://")) {
            seg = normalizeHostInNetworkStyleSegment(seg);
        }
        return seg;
    }

    /** {@code jdbc:xxx://host:port/db?params} 形态（MySQL、PG、DB2、Firebird 等常见写法）。 */
    private static String authoritySlashDatabase(String jdbcUrlLowercase) {
        int idx = jdbcUrlLowercase.indexOf("://");
        if (idx < 0) {
            return jdbcUrlLowercase;
        }
        String rest = jdbcUrlLowercase.substring(idx + 3);
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return normalizeAuthorityLoopbackHost(rest.trim());
        }
        String authority = normalizeAuthorityLoopbackHost(rest.substring(0, slash).trim());
        String afterSlash = rest.substring(slash + 1);
        int end = firstSemicolonOrQuestion(afterSlash);
        String db = (end >= 0 ? afterSlash.substring(0, end) : afterSlash).trim();
        return authority + "/" + db;
    }

    private static String sqlServerPhysicalKey(String jdbcUrlLowercase) {
        int start = jdbcUrlLowercase.indexOf("://");
        if (start < 0) {
            return jdbcUrlLowercase;
        }
        String rest = jdbcUrlLowercase.substring(start + 3);
        int firstSemi = rest.indexOf(';');
        String serverPart = normalizeAuthorityLoopbackHost(
                (firstSemi >= 0 ? rest.substring(0, firstSemi) : rest).trim());
        String dbName = "";
        for (String raw : rest.split(";")) {
            String p = raw.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            if ("databasename".equals(key) || "database".equals(key)) {
                dbName = p.substring(eq + 1).trim();
                break;
            }
        }
        return serverPart + "|" + dbName;
    }

    private static String oracleThinPhysicalKey(String jdbcUrlLowercase) {
        String rest = jdbcUrlLowercase.substring("jdbc:oracle:thin:".length()).trim();
        int at = rest.indexOf('@');
        if (at >= 0) {
            rest = rest.substring(at + 1);
        }
        int q = rest.indexOf('?');
        if (q >= 0) {
            rest = rest.substring(0, q);
        }
        rest = rest.trim();
        return normalizeOracleThinEndpoint(rest);
    }

    /**
     * {@code //host:port/service} 或 {@code host:port:sid} 等简单 Thin 串中的 {@code localhost} 与 {@code 127.0.0.1} 对齐。
     */
    private static String normalizeOracleThinEndpoint(String restLowercase) {
        if (restLowercase.startsWith("//")) {
            int slash = restLowercase.indexOf('/', 2);
            if (slash > 2) {
                String auth = restLowercase.substring(2, slash);
                return "//" + normalizeAuthorityLoopbackHost(auth) + restLowercase.substring(slash);
            }
            return "//" + normalizeAuthorityLoopbackHost(restLowercase.substring(2));
        }
        int c0 = restLowercase.indexOf(':');
        if (c0 > 0) {
            String host = restLowercase.substring(0, c0);
            if ("localhost".equals(host)) {
                return "127.0.0.1" + restLowercase.substring(c0);
            }
        }
        return restLowercase;
    }

    /**
     * 形如 {@code tcp://host:port/path} 的段：只规范化 {@code //} 与首个 {@code /} 之间的 authority。
     */
    private static String normalizeHostInNetworkStyleSegment(String segmentLowercase) {
        int p = segmentLowercase.indexOf("://");
        if (p < 0) {
            return segmentLowercase;
        }
        int hostStart = p + 3;
        int slash = segmentLowercase.indexOf('/', hostStart);
        if (slash < 0) {
            String auth = segmentLowercase.substring(hostStart);
            return segmentLowercase.substring(0, hostStart) + normalizeAuthorityLoopbackHost(auth);
        }
        String auth = segmentLowercase.substring(hostStart, slash);
        return segmentLowercase.substring(0, hostStart)
                + normalizeAuthorityLoopbackHost(auth)
                + segmentLowercase.substring(slash);
    }

    /**
     * JDBC authority 段（可含多主机 {@code host1,host2:port}）：将独立主机名 {@code localhost} 换为 {@code 127.0.0.1}。
     */
    private static String normalizeAuthorityLoopbackHost(String authority) {
        if (authority == null || authority.isEmpty()) {
            return authority;
        }
        String s = authority;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        final String needle = "localhost";
        while (true) {
            int p = s.indexOf(needle, i);
            if (p < 0) {
                sb.append(s.substring(i));
                break;
            }
            if (isLocalhostHostnameToken(s, p, needle.length())) {
                sb.append(s, i, p);
                sb.append("127.0.0.1");
                i = p + needle.length();
            } else {
                sb.append(s, i, p + 1);
                i = p + 1;
            }
        }
        return sb.toString();
    }

    /** {@code localhost} 作为主机名片段（非 {@code xxxlocalhost} / {@code localhostxxx} / 路径目录名误判尽量少）。 */
    private static boolean isLocalhostHostnameToken(String s, int p, int len) {
        if (p > 0) {
            char b = s.charAt(p - 1);
            if (isHostnameIdentContinuation(b)) {
                return false;
            }
        }
        int after = p + len;
        if (after < s.length()) {
            char c = s.charAt(after);
            if (isHostnameIdentContinuation(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHostnameIdentContinuation(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
    }

    private static int firstSemicolonOrQuestion(String s) {
        int semi = s.indexOf(';');
        int q = s.indexOf('?');
        if (semi < 0) {
            return q;
        }
        if (q < 0) {
            return semi;
        }
        return Math.min(semi, q);
    }
}
