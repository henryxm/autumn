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

    private InstallMode() {
    }

    public static boolean isActive(Environment environment) {
        return environment != null && Boolean.parseBoolean(environment.getProperty(PROPERTY, "false"));
    }
}
