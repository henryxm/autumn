package cn.org.autumn.install;

import cn.org.autumn.annotation.AllowPostConstructDuringInstall;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * 替换默认 {@link CommonAnnotationBeanPostProcessor}：在安装模式下不调用 {@link javax.annotation.PostConstruct}
 *（及父类中与之同一套生命周期的 init 回调），避免业务到处写 {@link InstallMode#isActive}。
 * <p>
 * {@link javax.annotation.Resource} / EJB 等注入仍由本类自父类继承的其它回调处理，不受安装模式影响。
 * 若个别 Bean 在安装期仍必须执行 {@code @PostConstruct}，在其<b>实现类</b>上标
 * {@link AllowPostConstructDuringInstall}。
 * <p>
 * <b>注意</b>：{@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet}、XML {@code init-method}
 * 仍由容器在其它阶段调用，不受本处理器影响；安装期若也要跳过，请改用条件配置或延迟到
 * {@link org.springframework.boot.ApplicationRunner}。
 * 跳过 {@code @PostConstruct} 时，父类通常为 {@code @PreDestroy} 注册的销毁回调也可能一并省略，安装完成若以
 * 向导内 JVM 重启/新 Context 收束则无妨。
 */
public class InstallModeAwareCommonAnnotationBeanPostProcessor extends CommonAnnotationBeanPostProcessor {

    @Autowired(required = false)
    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (environment != null && InstallMode.isActive(environment)) {
            Class<?> userClass = ClassUtils.getUserClass(bean);
            if (!AnnotatedElementUtils.hasAnnotation(userClass, AllowPostConstructDuringInstall.class)) {
                return bean;
            }
        }
        return super.postProcessBeforeInitialization(bean, beanName);
    }
}
