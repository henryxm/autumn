package cn.org.autumn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 无 Session API 认证标记（类 / 方法 / 参数均可标注）。
 * <ul>
 *   <li>{@link #notNull()}：是否强制要求有效身份（默认 true，缺失时 -10000）</li>
 *   <li>{@link #subject()}：参数为 {@code SysUserEntity} 时注入数据主体（机器人场景为 owner）</li>
 * </ul>
 * <p>
 * 与 {@link cn.org.autumn.model.UserContext} 配合：控制器直接声明 {@code UserContext} 参数即可注入，
 * 是否必填由本注解的 {@link #notNull()} 决定（可标在方法或参数上）。
 * <p>
 * <b>与遗留 {@code UserInfo} 并存</b>：参数类型为 minclouds {@code UserInfo} 时仍由 {@code UserInfoResolver} 解析
 * （即使参数带本注解）；新接口请改用 {@code UserContext} 或 {@code @Authenticated User} / {@code SysUserEntity}。
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {

    /**
     * 是否要求认证结果非空。
     */
    boolean notNull() default true;

    /**
     * 仅对 {@code SysUserEntity} 参数生效：为 true 时注入数据归属用户（机器人=主人），否则注入真人调用者本身。
     */
    boolean subject() default false;
}