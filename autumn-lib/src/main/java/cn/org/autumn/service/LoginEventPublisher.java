package cn.org.autumn.service;

import cn.org.autumn.config.LoginListenerHandler;
import cn.org.autumn.listener.LoginListener;
import cn.org.autumn.model.LoginEvent;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.Uuid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 用户登录事件发布器
 * <p>
 * 负责发布登录事件并异步通知所有注册的监听器
 * 采用异步处理机制，防止长时间占用线程导致流程死锁
 *
 * @author Autumn
 */
@Slf4j
@Service
public class LoginEventPublisher implements InitFactory.Must {

    @Autowired(required = false)
    private List<LoginListenerHandler> loginListenerHandlers;

    @Autowired(required = false)
    private AsyncTaskExecutor asyncTaskExecutor;

    /**
     * 所有注册的登录监听器
     */
    private final List<LoginListener> listeners = new ArrayList<>();

    /**
     * 实例ID，用于标识当前实例
     */
    private final String instance = Uuid.uuid();

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    @Override
    public void must() {
        init();
    }

    public void init() {
        if (initialized) {
            return;
        }
        // 确保监听器已收集（延迟初始化）
        collect();
        // 延迟初始化，等待所有Bean都注册完成
        // 使用懒加载方式收集监听器，避免@PostConstruct时Bean还未完全注册
        initialized = true;
        if (log.isDebugEnabled()) {
            log.debug("登录事件发布器初始化完成，将在首次使用时收集监听器");
        }
    }

    /**
     * 延迟收集监听器
     * <p>
     * 在首次发布事件时才收集所有监听器，确保所有Bean都已注册完成
     * 使用同步方法确保线程安全，避免并发初始化问题
     */
    private synchronized void collect() {
        // 如果已经收集过，直接返回
        if (!listeners.isEmpty()) {
            return;
        }
        // 收集所有登录监听器
        if (loginListenerHandlers != null && !loginListenerHandlers.isEmpty()) {
            for (LoginListenerHandler handler : loginListenerHandlers) {
                try {
                    LoginListener listener = handler.getListener();
                    if (listener != null && !listeners.contains(listener)) {
                        listeners.add(listener);
                        if (log.isDebugEnabled()) {
                            log.debug("注册监听: {}", listener.getClass().getName());
                        }
                    }
                } catch (Exception e) {
                    log.error("监听失败: {}", handler.getClass().getName(), e);
                }
            }
        }
        if (!listeners.isEmpty() && log.isDebugEnabled()) {
            log.debug("登录监听，共注册 {} 个监听器", listeners.size());
        }
    }

    /**
     * 发布登录事件
     * <p>
     * 异步通知所有注册的监听器，不会阻塞当前线程
     * 如果事件没有设置eventId和instanceId，会自动生成
     *
     * @param event 登录事件
     */
    public void publish(LoginEvent event) {
        if (event == null) {
            log.debug("登录事件为空，忽略发布");
            return;
        }
        // 确保事件ID和实例ID已设置
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(Uuid.uuid());
        }
        if (event.getInstance() == null || event.getInstance().isEmpty()) {
            event.setInstance(instance);
        }
        // 如果监听器列表为空，直接返回
        if (listeners.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("没有注册的登录监听器，忽略事件: {}", event.getId());
            }
            return;
        }
        // 异步处理，避免阻塞
        if (asyncTaskExecutor != null) {
            asyncTaskExecutor.execute(() -> notify(event));
        } else {
            // 如果没有异步执行器，使用CompletableFuture的默认线程池
            CompletableFuture.runAsync(() -> notify(event))
                    .exceptionally(throwable -> {
                        log.error("通知失败:{}", throwable.getMessage());
                        return null;
                    });
        }
    }

    /**
     * 通知所有监听器
     * <p>
     * 遍历所有注册的监听器，逐个调用onLogin方法
     * 如果某个监听器抛出异常，不影响其他监听器的执行
     *
     * @param event 登录事件
     */
    private void notify(LoginEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("通知监听，事件ID: {}, 监听器数量: {}", event.getId(), listeners.size());
        }
        for (LoginListener listener : listeners) {
            try {
                long startTime = System.currentTimeMillis();
                listener.onLogin(event);
                long duration = System.currentTimeMillis() - startTime;
                if (log.isDebugEnabled()) {
                    log.debug("登录监听器 {} 处理完成，耗时: {}ms", listener.getClass().getName(), duration);
                }
                // 如果处理时间过长，记录警告
                if (duration > 1000) {
                    log.debug("登录监听器 {} 处理时间过长: {}ms", listener.getClass().getName(), duration);
                }
            } catch (Exception e) {
                log.error("登录监听器 {} 处理事件失败，事件ID: {}", listener.getClass().getName(), event.getId(), e);
                // 继续执行其他监听器，不中断流程
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("登录事件通知完成，事件ID: {}", event.getId());
        }
    }

    /**
     * 手动注册监听器
     * <p>
     * 用于动态注册监听器，通常在初始化完成后调用
     *
     * @param listener 登录监听器
     */
    public void register(LoginListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (log.isDebugEnabled())
                log.debug("手动注册登录监听器: {}", listener.getClass().getName());
        }
    }

    /**
     * 移除监听器
     *
     * @param listener 要移除的监听器
     */
    public void unregister(LoginListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            if (log.isDebugEnabled())
                log.debug("移除登录监听器: {}", listener.getClass().getName());
        }
    }

    /**
     * 获取已注册的监听器数量
     *
     * @return 监听器数量
     */
    public int getCount() {
        return listeners.size();
    }

    /**
     * 获取实例ID
     *
     * @return 实例ID
     */
    public String getInstance() {
        return instance;
    }

    /**
     * 检查是否已初始化
     *
     * @return true表示已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
