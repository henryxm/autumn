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

    public static final String SESSION_AGREED = "INSTALL_AGREED";

    public static final String SESSION_FORM = "INSTALL_FORM";

    public static final String SESSION_CHECKS_OK = "INSTALL_CHECKS_OK";

    private InstallConstants() {
    }
}
