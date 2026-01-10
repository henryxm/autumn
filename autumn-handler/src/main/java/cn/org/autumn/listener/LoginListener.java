package cn.org.autumn.listener;

import cn.org.autumn.model.LoginEvent;

/**
 * 用户登录监听器接口
 * <p>
 * 实现此接口的类将被自动注册到登录事件系统中，当用户登录时会被调用
 * <p>
 * 注意：监听器的执行是异步的，不会阻塞登录流程，但应避免长时间运行的操作
 *
 * @author Autumn
 */
public interface LoginListener {
    /**
     * 处理用户登录事件
     * <p>
     * 当用户登录成功时，系统会异步调用此方法
     * <p>
     * 实现此方法时应该：
     * 1. 快速处理，避免长时间占用线程
     * 2. 处理异常，避免影响其他监听器
     * 3. 不要修改事件对象，只读取信息
     *
     * @param event 登录事件，包含用户信息、登录时间、来源等
     */
    void onLogin(LoginEvent event);
}
