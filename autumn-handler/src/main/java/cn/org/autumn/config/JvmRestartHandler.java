package cn.org.autumn.config;

import org.springframework.core.annotation.Order;

/**
 * 同一 JVM 内 {@link org.springframework.boot.SpringApplication#exit} 关闭 Spring 上下文之后、
 * 再次 {@link org.springframework.boot.SpringApplication#run} 之前需要执行的清理逻辑（例如第三方库的 static 缓存）。
 * <p>
 * <b>约定</b>
 * <ul>
 *   <li>实现类注册为 Spring Bean；由应用内协调器在上下文仍存活时收集实例，
 *       在 {@code SpringApplication.exit} 之后按 {@link Order} 顺序调用，与具体数据库类型无关。</li>
 *   <li>{@link #cleanAfterContextClosed()} 内<b>禁止</b>再访问已关闭的 {@code ApplicationContext} / BeanFactory，
 *       仅允许清理 JVM 静态状态、线程局部变量等。</li>
 *   <li>执行顺序：在接口方法或实现类方法上使用 {@link Order}（值越小越先执行）。</li>
 * </ul>
 */
public interface JvmRestartHandler {

    /**
     * Spring 上下文已关闭后调用；不得依赖 Spring 容器。
     */
    @Order(Integer.MAX_VALUE / 1000)
    void cleanAfterContextClosed();
}
