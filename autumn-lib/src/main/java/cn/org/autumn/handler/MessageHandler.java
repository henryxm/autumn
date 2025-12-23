package cn.org.autumn.handler;

/**
 * 消息处理器接口
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * 处理接收到的消息
     *
     * @param channel 频道名称
     * @param messageBody 消息体（JSON字符串）
     */
    void handle(String channel, String messageBody);
}