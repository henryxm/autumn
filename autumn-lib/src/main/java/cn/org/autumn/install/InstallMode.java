package cn.org.autumn.install;

import org.springframework.core.env.Environment;

/**
 * 安装向导占位启动：{@code autumn.install.mode=true} 时应用尚未加载真实 {@code config/datasource.yml}，
 * 业务侧应跳过依赖「真实库类型」的初始化与 DDL。
 * <p>
 * 框架通过 {@link InstallModeAwareCommonAnnotationBeanPostProcessor} 在安装模式下统一跳过
 * {@link javax.annotation.PostConstruct}；少数仍须在安装期执行的 Bean 请标
 * {@link cn.org.autumn.annotation.AllowPostConstructDuringInstall}。
 */
public final class InstallMode {

    public static final String PROPERTY = "autumn.install.mode";

    /**
     * 在 {@link org.springframework.boot.env.EnvironmentPostProcessor} 中绑定，早于 Bean 创建；
     * {@link cn.org.autumn.install.InstallModeAwareCommonAnnotationBeanPostProcessor} 等无法注入 {@link Environment} 时仍可读安装状态。
     */
    private static volatile Environment rootEnvironment;

    private InstallMode() {
    }

    /**
     * 供启动链在最早阶段写入；多次调用时覆盖为当前 {@link org.springframework.context.ConfigurableEnvironment}。
     */
    public static void setRootEnvironment(Environment environment) {
        if (environment != null) {
            rootEnvironment = environment;
        }
    }

    /**
     * 等价于 {@code isActive(null)}：使用 {@link #setRootEnvironment} 绑定的环境。
     */
    public static boolean isActive() {
        return isActive(null);
    }

    /**
     * @param environment 非空时优先使用；为空则回退到 {@link #setRootEnvironment} 绑定的根环境
     */
    public static boolean isActive(Environment environment) {
        Environment e = environment != null ? environment : rootEnvironment;
        return e != null && Boolean.parseBoolean(e.getProperty(PROPERTY, "false"));
    }
}
