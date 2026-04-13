package cn.org.autumn.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在<b>具体 Bean 类型</b>上：当 {@code autumn.install.mode=true} 时，全局会跳过
 * {@link javax.annotation.PostConstruct}（见 {@code InstallModeAwareCommonAnnotationBeanPostProcessor}），
 * 被标注的类型仍执行 {@code @PostConstruct}。用于安装向导仍依赖的极少数初始化（例如特定 Web 配置）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AllowPostConstructDuringInstall {
}
