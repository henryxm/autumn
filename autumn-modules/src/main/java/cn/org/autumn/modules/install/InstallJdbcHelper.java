package cn.org.autumn.modules.install;

import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.modules.install.dto.InstallConnectionForm;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * 按 {@link DatabaseType} 推断驱动类与 JDBC URL（安装向导「简易填写」模式）。
 * <p>
 * 连接串会为常见数据库尽量带上利于存储 Emoji、多语言文本的参数（如 MySQL 的 {@code connectionCollation=utf8mb4_unicode_ci}、
 * PostgreSQL 的 {@code stringtype=unspecified}、SQL Server 的 {@code sendStringParametersAsUnicode=true}、Firebird 的 {@code encoding=UTF8} 等）。
 * 高级模式由用户自写 URL 时，需自行保证库与连接字符集一致。
 */
public final class InstallJdbcHelper {

    public enum ConnectionMode {
        REMOTE,
        EMBEDDED_FILE,
        EMBEDDED_MEMORY;

        static ConnectionMode fromForm(InstallConnectionForm form, DatabaseType type) {
            String raw = form.getConnectionMode();
            if (StringUtils.isNotBlank(raw)) {
                try {
                    return valueOf(raw.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("无效连接方式，请选远程、本机文件或内存库对应项");
                }
            }
            if (type == DatabaseType.H2 || type == DatabaseType.SQLITE || type == DatabaseType.HSQLDB || type == DatabaseType.DERBY) {
                return EMBEDDED_FILE;
            }
            return REMOTE;
        }
    }

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

    public static boolean supportsEmbeddedFile(DatabaseType type) {
        return type == DatabaseType.H2 || type == DatabaseType.SQLITE || type == DatabaseType.HSQLDB || type == DatabaseType.DERBY;
    }

    public static boolean supportsEmbeddedMemory(DatabaseType type) {
        return type == DatabaseType.H2 || type == DatabaseType.SQLITE || type == DatabaseType.HSQLDB || type == DatabaseType.DERBY;
    }

    public static String databaseFieldHint(DatabaseType type, ConnectionMode mode) {
        if (type == null) {
            return "按所选类型填写";
        }
        if (mode == ConnectionMode.EMBEDDED_MEMORY) {
            switch (type) {
                case H2:
                    return "内存库名称，可留空为 autumn";
                case SQLITE:
                    return "内存模式无需路径，可留空";
                case HSQLDB:
                    return "内存库名，可留空为 autumn";
                case DERBY:
                    return "内存库名，可留空为 autumn";
                default:
                    return "该类型不支持内存简易模式，请换类型或高级模式";
            }
        }
        if (mode == ConnectionMode.EMBEDDED_FILE) {
            switch (type) {
                case H2:
                    return "文件路径，可留空为 ./data/autumn（相对运行目录）";
                case SQLITE:
                    return "数据库文件路径，例如 ./data/app.db";
                case HSQLDB:
                    return "文件路径前缀，可留空为 autumn（生成 hsqldb 文件）";
                case DERBY:
                    return "目录路径，可留空为 data/autumn-derby";
                default:
                    return "库名或 SID 等";
            }
        }
        switch (type) {
            case MYSQL:
            case MARIADB:
            case TIDB:
            case OCEANBASE_MYSQL:
                return "数据库名，例如 autumn";
            case POSTGRESQL:
            case KINGBASE:
                return "数据库名";
            case SQLSERVER:
                return "数据库名";
            case ORACLE:
            case OCEANBASE_ORACLE:
                return "SID 或 Service 名";
            case FIREBIRD:
                return "服务器上的 .fdb 文件路径";
            case DB2:
                return "数据库名";
            case DAMENG:
                return "库名或服务名";
            case H2:
                return "H2 库路径（TCP 模式下为服务器端路径标识）";
            case HSQLDB:
                return "HSQLDB 库别名（网络模式）";
            default:
                return "库名、路径或连接标识";
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

        ConnectionMode mode = ConnectionMode.fromForm(form, type);
        ensureModeSupported(type, mode);

        String host = StringUtils.defaultString(form.getHost(), "localhost").trim();
        String db = StringUtils.defaultString(form.getDatabaseName(), "").trim();

        int port = parsePort(form.getPort(), defaultPort(type));
        String driver = StringUtils.isNotBlank(form.getDriverClassName())
                ? form.getDriverClassName().trim()
                : defaultDriver(type);
        if (StringUtils.isBlank(driver)) {
            throw new IllegalArgumentException("无法推断驱动类名，请使用高级模式填写驱动与 URL，或将驱动加入依赖");
        }

        String url;
        switch (mode) {
            case EMBEDDED_MEMORY:
                url = buildEmbeddedMemoryUrl(type, db);
                break;
            case EMBEDDED_FILE:
                url = buildEmbeddedFileUrl(type, db);
                break;
            case REMOTE:
            default:
                url = buildRemoteUrl(type, host, port, db);
                break;
        }

        url = appendExtraOptions(url, form.getExtraOptions());

        return new ResolvedJdbc(url, driver, type);
    }

    private static String appendExtraOptions(String url, String extraOptions) {
        if (StringUtils.isBlank(extraOptions)) {
            return url;
        }
        String e = extraOptions.trim();
        if (url.startsWith("jdbc:sqlserver")) {
            return url.endsWith(";") ? url + e : url + ";" + e;
        }
        if (url.startsWith("jdbc:h2") || url.startsWith("jdbc:hsqldb") || url.startsWith("jdbc:derby")) {
            return url + ";" + e;
        }
        if (url.startsWith("jdbc:sqlite")) {
            return url.contains("?") ? url + "&" + e : url + "?" + e;
        }
        if (url.contains("?")) {
            return url + "&" + e;
        }
        return url + "?" + e;
    }

    private static void ensureModeSupported(DatabaseType type, ConnectionMode mode) {
        if (mode == ConnectionMode.EMBEDDED_FILE && !supportsEmbeddedFile(type)) {
            throw new IllegalArgumentException("当前数据库类型不支持「本机文件」简易模式，请选远程或高级 JDBC");
        }
        if (mode == ConnectionMode.EMBEDDED_MEMORY && !supportsEmbeddedMemory(type)) {
            throw new IllegalArgumentException("当前数据库类型不支持「内存库」简易模式，请换库或高级 JDBC");
        }
    }

    private static String buildEmbeddedMemoryUrl(DatabaseType type, String db) {
        switch (type) {
            case H2:
                if (StringUtils.isBlank(db)) {
                    db = "autumn";
                }
                return "jdbc:h2:mem:" + db + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
            case SQLITE:
                return "jdbc:sqlite::memory:";
            case HSQLDB:
                if (StringUtils.isBlank(db)) {
                    db = "autumn";
                }
                return "jdbc:hsqldb:mem:" + db;
            case DERBY:
                if (StringUtils.isBlank(db)) {
                    db = "autumn";
                }
                return "jdbc:derby:memory:" + db + ";create=true";
            default:
                throw new IllegalArgumentException("不支持该类型的内存连接");
        }
    }

    private static String buildEmbeddedFileUrl(DatabaseType type, String db) {
        switch (type) {
            case H2:
                if (StringUtils.isBlank(db)) {
                    db = "./data/autumn";
                }
                return "jdbc:h2:file:" + db + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
            case SQLITE:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 SQLite 数据库文件路径");
                }
                return "jdbc:sqlite:" + db;
            case HSQLDB:
                if (StringUtils.isBlank(db)) {
                    db = "autumn";
                }
                return "jdbc:hsqldb:file:" + db + ";shutdown=true";
            case DERBY:
                if (StringUtils.isBlank(db)) {
                    db = "data/autumn-derby";
                }
                return "jdbc:derby:" + db + ";create=true";
            default:
                throw new IllegalArgumentException("不支持该类型的本机文件连接");
        }
    }

    private static String buildRemoteUrl(DatabaseType type, String host, int port, String db) {
        switch (type) {
            case MYSQL:
            case MARIADB:
            case TIDB:
            case OCEANBASE_MYSQL:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                return "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?serverTimezone=Asia/Shanghai&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&connectionCollation=utf8mb4_unicode_ci";
            case POSTGRESQL:
            case KINGBASE:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                return "jdbc:postgresql://" + host + ":" + port + "/" + db + "?stringtype=unspecified";
            case SQLSERVER:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + db
                        + ";encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true";
            case ORACLE:
            case OCEANBASE_ORACLE:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 SID / Service 名（作为 database 字段）");
                }
                return "jdbc:oracle:thin:@" + host + ":" + port + ":" + db;
            case SQLITE:
                throw new IllegalArgumentException("SQLite 请使用「本机文件」或「内存」，或高级 JDBC");
            case H2: {
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 H2 数据库名或路径标识");
                }
                int p = port > 0 ? port : 9092;
                return "jdbc:h2:tcp://" + host + ":" + p + "/" + db.replace("\\", "/") + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
            }
            case HSQLDB: {
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 HSQLDB 库名");
                }
                int p = port > 0 ? port : 9001;
                return "jdbc:hsqldb:hsql://" + host + ":" + p + "/" + db;
            }
            case DERBY:
                throw new IllegalArgumentException("Derby 远程连接请使用高级模式填写 JDBC");
            case DB2:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名");
                }
                return "jdbc:db2://" + host + ":" + port + "/" + db;
            case FIREBIRD:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写 Firebird 文件路径");
                }
                return "jdbc:firebirdsql://" + host + ":" + port + "/" + db.replace("\\", "/") + "?encoding=UTF8";
            case INFORMIX:
                throw new IllegalArgumentException("Informix 请使用「高级模式」填写完整 JDBC URL 与 INFORMIXSERVER 等参数");
            case DAMENG:
                if (StringUtils.isBlank(db)) {
                    throw new IllegalArgumentException("请填写数据库名或连接串中的库名");
                }
                return "jdbc:dm://" + host + ":" + port + "/" + db;
            default:
                throw new IllegalArgumentException("该类型请使用「高级模式」直接填写 JDBC URL 与驱动类");
        }
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
