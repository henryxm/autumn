package cn.org.autumn.config;

import cn.org.autumn.listener.LoginListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 登录监听器处理器接口
 * <p>
 * 实现此接口的类会被自动扫描和注册，用于提供登录监听器
 * 类似于 InterceptorHandler、DeleteHandler 等
 *
 * @author Autumn
 */
@Component
@ConditionalOnMissingBean(LoginListenerHandler.class)
public interface LoginListenerHandler {
    /**
     * 获取登录监听器实例
     * <p>
     * 如果返回null，表示当前Handler不提供监听器
     *
     * @return 登录监听器实例，或null
     */
    default LoginListener getListener() {
        return null;
    }
}
