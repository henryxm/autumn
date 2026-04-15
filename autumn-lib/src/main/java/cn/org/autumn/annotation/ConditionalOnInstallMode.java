package cn.org.autumn.annotation;

import cn.org.autumn.install.condition.InstallModeActiveCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仅当 {@code autumn.install.mode=true} 时匹配，用于仅在安装向导阶段注册的 Bean 或配置类。
 *
 * @see ConditionalOnNotInstallMode
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(InstallModeActiveCondition.class)
public @interface ConditionalOnInstallMode {
}
