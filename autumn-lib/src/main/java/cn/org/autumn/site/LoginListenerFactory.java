package cn.org.autumn.site;

import cn.org.autumn.config.LoginListenerHandler;
import cn.org.autumn.listener.LoginListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录监听器工厂
 * <p>
 * 管理和调用所有登录监听器
 * 类似于 DeleteFactory、AccountFactory 等
 * <p>
 * 注意：实际的监听器注册和管理由 LoginEventPublisher 负责
 * 此类主要用于提供统一的访问入口
 *
 * @author Autumn
 */
@Slf4j
@Component
public class LoginListenerFactory extends Factory {

    /**
     * 获取登录监听器列表
     * <p>
     * 从所有LoginListenerHandler中收集监听器
     *
     * @return 登录监听器列表
     */
    public List<LoginListener> getListeners() {
        List<LoginListener> listeners = new ArrayList<>();
        List<LoginListenerHandler> handlers = getOrderList(LoginListenerHandler.class);

        if (handlers != null && !handlers.isEmpty()) {
            for (LoginListenerHandler handler : handlers) {
                // 跳过自己，避免循环引用
                if (handler instanceof LoginListenerFactory) {
                    continue;
                }
                try {
                    LoginListener listener = handler.getListener();
                    if (listener != null) {
                        listeners.add(listener);
                    }
                } catch (Exception e) {
                    log.error("获取登录监听器失败: {}", handler.getClass().getName(), e);
                }
            }
        }
        return listeners;
    }
}