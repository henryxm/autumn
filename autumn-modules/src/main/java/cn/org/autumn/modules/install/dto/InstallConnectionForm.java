package cn.org.autumn.modules.install.dto;

import java.io.Serializable;

/**
 * 安装向导中用户提交的数据库连接参数。
 */
public class InstallConnectionForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /** {@link cn.org.autumn.database.DatabaseType} 名称 */
    private String databaseType;

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

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
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
}
