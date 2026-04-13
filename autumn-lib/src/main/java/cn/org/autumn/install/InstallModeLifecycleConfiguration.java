package cn.org.autumn.install;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * 用 {@link InstallModeAwareCommonAnnotationBeanPostProcessor} 替换框架默认的
 * {@code internalCommonAnnotationProcessor}，使安装模式下 {@code @PostConstruct} 可统一抑制。
 */
@Configuration(proxyBeanMethods = false)
public class InstallModeLifecycleConfiguration {

    @Bean
    public static InstallModeCommonAnnotationProcessorRegistrar installModeCommonAnnotationProcessorRegistrar() {
        return new InstallModeCommonAnnotationProcessorRegistrar();
    }

    public static final class InstallModeCommonAnnotationProcessorRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered {

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) {
            String name = AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME;
            if (registry.containsBeanDefinition(name)) {
                registry.removeBeanDefinition(name);
            }
            RootBeanDefinition def = new RootBeanDefinition(InstallModeAwareCommonAnnotationBeanPostProcessor.class);
            def.setRole(org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(name, def);
        }

        @Override
        public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }
    }
}
