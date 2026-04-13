package cn.org.autumn.install;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 将当前 {@link ConfigurableEnvironment} 绑定到 {@link InstallMode}，时机早于任何 Bean（含
 * {@link InstallModeAwareCommonAnnotationBeanPostProcessor}）创建，避免后者上 {@code @Autowired Environment} 仍为 null。
 * <p>
 * 使用 {@link Ordered#LOWEST_PRECEDENCE}，保证在其它 {@link EnvironmentPostProcessor}（例如写入
 * {@code autumn.install.mode}）之后执行，绑定对象上已能读到最终属性。
 */
public class InstallModeEnvironmentBindingPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        InstallMode.setRootEnvironment(environment);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
