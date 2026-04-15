package cn.org.autumn.annotation;

import cn.org.autumn.install.condition.NotInstallModeCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当 {@code autumn.install.mode=true}（安装向导占位启动）时<b>不匹配</b>，从而不注册该配置类或该 {@code @Bean}。
 * <p>
 * 用于避免安装期创建依赖真实业务库、远程服务或重型初始化的 Bean；判定使用
 * {@link org.springframework.context.annotation.ConditionContext#getEnvironment()}，与
 * {@link cn.org.autumn.install.InstallModeEnvironmentBindingPostProcessor} 绑定的静态根环境无关。
 * <p>
 * 与 {@link cn.org.autumn.install.InstallModeAwareCommonAnnotationBeanPostProcessor} 跳过
 * {@link javax.annotation.PostConstruct} 互补：本注解阻止 Bean 定义生效；后者仅抑制已创建 Bean 的 PostConstruct。
 *
 * @see ConditionalOnInstallMode
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(NotInstallModeCondition.class)
public @interface ConditionalOnNotInstallMode {
}
