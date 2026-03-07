package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 请求菜单权限标记。
 * <p>
 * 标注在接口方法上，声明该请求关联的菜单/权限键集合，
 * 供权限拦截或审计模块进行鉴权与记录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMenu {
    /**
     * 菜单/权限键数组。
     */
    String[] value() default "";
}