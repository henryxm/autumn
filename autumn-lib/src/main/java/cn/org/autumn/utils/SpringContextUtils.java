package cn.org.autumn.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring Context 工具类
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpringContextUtils.class);
    public static ApplicationContext applicationContext;
    private static DefaultListableBeanFactory defaultListableBeanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        try {
            SpringContextUtils.applicationContext = applicationContext;
            ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
            SpringContextUtils.defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
        } catch (Exception e) {
            log.error("Set Application Context:{}", e.getMessage());
        }
    }

    public static void registerBean(String beanName, Class<?> clazz) {
        try {
            if (StringUtils.isNotBlank(beanName) && null != clazz && null != defaultListableBeanFactory) {
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
                defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
            }
        } catch (Exception e) {
            log.error("Register Bean:{}", e.getMessage());
        }
    }

    public static void removeBean(String beanName) {
        try {
            if (StringUtils.isNotBlank(beanName) && null != defaultListableBeanFactory) {
                defaultListableBeanFactory.removeBeanDefinition(beanName);
            }
        } catch (Exception e) {
            log.error("Remove Bean:{}", e.getMessage());
        }
    }

    public static Object getBean(String name) {
        try {
            return applicationContext.getBean(name);
        } catch (Exception e) {
        }
        return null;
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return applicationContext.getBean(name, requiredType);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public static boolean isSingleton(String name) {
        return applicationContext.isSingleton(name);
    }

    public static Class<? extends Object> getType(String name) {
        return applicationContext.getType(name);
    }
}