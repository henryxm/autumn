package cn.org.autumn.modules.install.dto;

import java.io.Serializable;

/**
 * 安装向导中用户提交的数据库连接参数。
 */
public class InstallConnectionForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /** {@link cn.org.autumn.database.DatabaseType} 名称 */
    private String databaseType;

    /**
     * 简易模式连接方式：{@code REMOTE} 远程服务、{@code EMBEDDED_FILE} 本机文件、{@code EMBEDDED_MEMORY} 内存库。
     * 留空时由服务端按数据库类型推断（与旧版向导兼容）。
     */
    private String connectionMode;

    private String host = "localhost";

    private String port = "";

    private String databaseName = "";

    private String username = "";

    private String password = "";

    /** 可选：PostgreSQL schema、Oracle service 名补充等 */
    private String extraOptions = "";

    /** 是否直接使用完整 JDBC URL（高级） */
    private boolean advancedUrl;

    private String jdbcUrl = "";

    /** 高级：自定义驱动类，空则按数据库类型默认 */
    private String driverClassName = "";

    /**
     * H2 简易模式兼容选项：
     * <ul>
     *   <li>{@code MYSQL} / {@code NATIVE} / {@code POSTGRESQL} / {@code ORACLE} / {@code MSSQLSERVER} / {@code MARIADB}</li>
     * </ul>
     * 非 H2 类型可留空，服务端会忽略。
     */
    private String h2CompatibilityMode = "";

    /** 是否在安装后的配置中启用 Redis（会写入 {@code spring.redis.*} 与 {@code autumn.redis.open}）。 */
    private boolean enableRedis;

    /** 是否在启用 Redis 时把 Shiro Session 存到 Redis（需同时启用 Redis）。 */
    private boolean enableShiroRedis;

    private String redisHost = "127.0.0.1";

    private String redisPort = "6379";

    private String redisPassword = "";

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(String connectionMode) {
        this.connectionMode = connectionMode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExtraOptions() {
        return extraOptions;
    }

    public void setExtraOptions(String extraOptions) {
        this.extraOptions = extraOptions;
    }

    public boolean isAdvancedUrl() {
        return advancedUrl;
    }

    public void setAdvancedUrl(boolean advancedUrl) {
        this.advancedUrl = advancedUrl;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getH2CompatibilityMode() {
        return h2CompatibilityMode;
    }

    public void setH2CompatibilityMode(String h2CompatibilityMode) {
        this.h2CompatibilityMode = h2CompatibilityMode;
    }

    public boolean isEnableRedis() {
        return enableRedis;
    }

    public void setEnableRedis(boolean enableRedis) {
        this.enableRedis = enableRedis;
    }

    public boolean isEnableShiroRedis() {
        return enableShiroRedis;
    }

    public void setEnableShiroRedis(boolean enableShiroRedis) {
        this.enableShiroRedis = enableShiroRedis;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public String getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(String redisPort) {
        this.redisPort = redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }
}
