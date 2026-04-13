package cn.org.autumn.modules.install;

import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.modules.install.dto.InstallConnectionForm;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;

/**
 * 按 {@link DatabaseType} 推断驱动类与 JDBC URL（安装向导「简易填写」模式）。
 * <p>
 * 连接串会为常见数据库尽量带上利于存储 Emoji、多语言文本的参数（如 MySQL 的 {@code connectionCollation=utf8mb4_unicode_ci}、
 * PostgreSQL 的 {@code stringtype=unspecified}、SQL Server 的 {@code sendStringParametersAsUnicode=true}、Firebird 的 {@code encoding=UTF8} 等）。
 * 高级模式由用户自写 URL 时，需自行保证库与连接字符集一致。
 */
public final class InstallJdbcHelper {

    private InstallJdbcHelper() {
    }

    public static String defaultDriver(DatabaseType type) {
        if (type == null) {
            return "com.mysql.cj.jdbc.Driver";
        }
        switch (type) {
            case MYSQL:
            case TIDB:
            case OCEANBASE_MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case MARIADB:
                return "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL:
            case KINGBASE:
                return "org.postgresql.Driver";
            case SQLSERVER:
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case ORACLE:
            case OCEANBASE_ORACLE:
                return "oracle.jdbc.OracleDriver";
            case SQLITE:
                return "org.sqlite.JDBC";
            case H2:
                return "org.h2.Driver";
            case HSQLDB:
                return "org.hsqldb.jdbc.JDBCDriver";
            case DERBY:
                return "org.apache.derby.jdbc.EmbeddedDriver";
            case DB2:
                return "com.ibm.db2.jcc.DB2Driver";
            case FIREBIRD:
                return "org.firebirdsql.jdbc.FBDriver";
            case INFORMIX:
                return "com.informix.jdbc.IfxDriver";
            case DAMENG:
                return "dm.jdbc.driver.DmDriver";
            default:
                return "";
        }
    }

    public static int defaultPort(DatabaseType type) {
        if (type == null) {
            return 3306;
        }
        switch (type) {
            case MYSQL:
            case MARIADB:
            case TIDB:
            case OCEANBASE_MYSQL:
                return 3306;
            case POSTGRESQL:
            case KINGBASE:
                return 5432;
            case SQLSERVER:
                return 1433;
            case ORACLE:
            case OCEANBASE_ORACLE:
                return 1521;
            case FIREBIRD:
                return 3050;
            default:
                return 0;
        }
    }

    /**
     * 解析向导表单为 JDBC URL 与驱动类名。
     */
    public static ResolvedJdbc resolve(InstallConnectionForm form) throws IllegalArgumentException {
        if (form == null) {
            throw new IllegalArgumentException("表单为空");
        }
        DatabaseType type = parseType(form.getDatabaseType());
        if (form.isAdvancedUrl()) {
            if (StringUtils.isBlank(form.getJdbcUrl())) {
                throw new IllegalArgumentException("请填写完整 JDBC URL");
            }
            String driver = StringUtils.isNotBlank(form.getDriverClassName())
                    ? form.getDriverClassName().trim()
                    : defaultDriver(type);
            if (StringUtils.isBlank(driver)) {
                throw new IllegalArgumentException("该数据库类型需手动填写 JDBC 驱动类名，或将对应驱动 JAR 放入 classpath");
            }
            return new ResolvedJdbc(form.getJdbcUrl().trim(), driver, type);
        }

        String host = StringUtils.defaultString(form.getHost(), "localhost").trim();
        String db = StringUtils.defaultString(form.getDatabaseName(), "").trim();
        String user = form.getUsername() == null ? "" : form.getUsername().trim();

        int port = parsePort(form.getPort(), defaultPort(type));
        String driver = StringUtils.isNotBlank(form.getDriverClassName())
                ? form.getDriverClassName().trim()
                : defaultDriver(type);
        if (StringUtils.isBlank(driver)) {
            throw new IllegalArgumentException("无法推断驱动类名，请使用高级模式填写驱动与 URL，或将驱动加入依赖");
        }

        String url;
        switch (type) {
            case MYSQL:
            case MARIADB:
            case TIDB:
            case OCEANBASE_MYSQL:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                url = "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?serverTimezone=Asia/Shanghai&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&connectionCollation=utf8mb4_unicode_ci";
                break;
            case POSTGRESQL:
            case KINGBASE:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                url = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?stringtype=unspecified";
                break;
            case SQLSERVER:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                url = "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + db
                        + ";encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true";
                break;
            case ORACLE:
            case OCEANBASE_ORACLE:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 SID / Service 名（作为 database 字段）");
                }
                url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + db;
                break;
            case SQLITE:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 SQLite 文件路径（或 :memory:）");
                }
                url = "jdbc:sqlite:" + db;
                break;
            case H2:
                if (StringUtils.isBlank(db)) {
                    db = "./data/autumn";
                }
                url = "jdbc:h2:file:" + db + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
                break;
            case HSQLDB:
                if (StringUtils.isBlank(db)) {
                    db = "autumn";
                }
                url = "jdbc:hsqldb:file:" + db + ";shutdown=true";
                break;
            case DERBY:
                if (StringUtils.isBlank(db)) {
                    db = "data/autumn-derby";
                }
                url = "jdbc:derby:" + db + ";create=true";
                break;
            case DB2:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                url = "jdbc:db2://" + host + ":" + port + "/" + db;
                break;
            case FIREBIRD:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 Firebird 文件路径");
                }
                url = "jdbc:firebirdsql://" + host + ":" + port + "/" + db.replace("\\", "/") + "?encoding=UTF8";
                break;
            case INFORMIX:
                throw new IllegalArgumentException("Informix 请使用「高级模式」填写完整 JDBC URL 与 INFORMIXSERVER 等参数");
            case DAMENG:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名或连接串中的库名");
                }
                url = "jdbc:dm://" + host + ":" + port + "/" + db;
                break;
            default:
                throw new IllegalArgumentException("该类型请使用「高级模式」直接填写 JDBC URL 与驱动类");
        }

        if (StringUtils.isNotBlank(form.getExtraOptions())) {
            String sep = url.contains("?") ? "&" : "?";
            url = url + sep + form.getExtraOptions().trim();
        }

        return new ResolvedJdbc(url, driver, type);
    }

    public static DatabaseType parseType(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new IllegalArgumentException("请选择数据库类型");
        }
        try {
            return DatabaseType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的数据库类型: " + raw);
        }
    }

    private static int parsePort(String portStr, int defaultPort) {
        if (StringUtils.isBlank(portStr)) {
            return defaultPort > 0 ? defaultPort : 3306;
        }
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("端口无效: " + portStr);
        }
    }

    public static final class ResolvedJdbc {
        private final String jdbcUrl;
        private final String driverClassName;
        private final DatabaseType databaseType;

        public ResolvedJdbc(String jdbcUrl, String driverClassName, DatabaseType databaseType) {
            this.jdbcUrl = jdbcUrl;
            this.driverClassName = driverClassName;
            this.databaseType = databaseType;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public DatabaseType getDatabaseType() {
            return databaseType;
        }
    }
}
