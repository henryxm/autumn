package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 登录校验标记。
 * <p>
 * 标注在控制器方法上，声明该接口需要登录态访问。
 * 具体校验逻辑由鉴权拦截器/登录工厂实现。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Login {
}
