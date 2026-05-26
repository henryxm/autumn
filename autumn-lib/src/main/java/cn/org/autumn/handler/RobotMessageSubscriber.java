package cn.org.autumn.handler;

import cn.org.autumn.model.RobotMessage;

/**
 * 机器人入站消息订阅者：业务模块实现本接口并注册为 Spring Bean，按 {@link #events()} 接收分发。
 * <p>
 * 与出站 {@link RobotHookDispatch}（HTTP 回调）并行：入站 API 会同时调用本接口与 Hook 队列。
 */
public interface RobotMessageSubscriber {

    /**
     * 订阅的消息类型，逗号分隔；{@code *} 表示全部（与 Hook events 规则一致）。
     */
    String events();

    /**
     * 处理入站消息（按 Factory {@link org.springframework.core.annotation.Order} 升序调用）。
     * {@link RobotMessage#getData()} 为 JSON 文本，业务侧自行解析。
     */
    void receive(RobotMessage message);
}
