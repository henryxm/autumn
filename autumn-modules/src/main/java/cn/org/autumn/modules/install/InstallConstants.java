package cn.org.autumn.modules.install;

/**
 * 安装向导相关配置键与固定值。
 */
public final class InstallConstants {

    public static final String INSTALL_MODE = "autumn.install.mode";

    public static final String WIZARD_ENABLED = "autumn.install.wizard";

    public static final String CONFIG_PATH = "autumn.install.config-path";

    public static final String FORCE_REINSTALL = "autumn.install.force";

    /**
     * 进入安装引导占位启动时，追加到 {@code spring.autoconfigure.exclude} 的自动配置类全名列表（逗号分隔），
     * 与已有 {@code spring.autoconfigure.exclude} 合并；仅当向导分支写入 {@code autumnInstallBootstrap} 时生效。
     */
    public static final String AUTOCONFIGURE_EXCLUDE_EXTRA = "autumn.install.autoconfigure-exclude";

    /** 与 {@link #CONFIG_PATH} 未配置时的默认相对路径一致（相对程序运行目录）。 */
    public static final String DEFAULT_CONFIG_FILE = "config/datasource.yml";

    /**
     * 安装占位数据源形态：{@value #BOOTSTRAP_FLAVOR_H2}（默认，内存 H2、无需外部库进程）或 {@value #BOOTSTRAP_FLAVOR_MYSQL}
     *（须本机 MySQL 已启动且 URL 可达，否则无法完成 Spring/MyBatis 启动）。
     */
    public static final String BOOTSTRAP_DATASOURCE_FLAVOR = "autumn.install.bootstrap-datasource.flavor";

    public static final String BOOTSTRAP_FLAVOR_H2 = "h2";

    public static final String BOOTSTRAP_FLAVOR_MYSQL = "mysql";

    /**
     * 安装占位数据源（无外部配置文件、进入向导分支时）：覆盖默认 JDBC URL（仅 {@link #BOOTSTRAP_FLAVOR_MYSQL} 时默认非空；
     * {@link #BOOTSTRAP_FLAVOR_H2} 使用固定内存 URL，一般无需设置）。
     */
    public static final String BOOTSTRAP_DATASOURCE_URL = "autumn.install.bootstrap-datasource.url";

    /** 占位数据源用户名，默认 {@code root}。 */
    public static final String BOOTSTRAP_DATASOURCE_USERNAME = "autumn.install.bootstrap-datasource.username";

    /** 占位数据源密码，默认空。 */
    public static final String BOOTSTRAP_DATASOURCE_PASSWORD = "autumn.install.bootstrap-datasource.password";

    /** 占位数据源驱动类名，默认 MySQL 8 驱动 {@code com.mysql.cj.jdbc.Driver}。 */
    public static final String BOOTSTRAP_DATASOURCE_DRIVER = "autumn.install.bootstrap-datasource.driver-class-name";

    public static final String SESSION_AGREED = "INSTALL_AGREED";

    public static final String SESSION_FORM = "INSTALL_FORM";

    public static final String SESSION_CHECKS_OK = "INSTALL_CHECKS_OK";

    private InstallConstants() {
    }
}
