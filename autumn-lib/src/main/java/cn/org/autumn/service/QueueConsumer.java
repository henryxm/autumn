package cn.org.autumn.service;

import cn.org.autumn.model.QueueMessage;

/**
 * 队列消费者接口
 * 用于处理队列中的消息
 *
 * @param <T> 消息体类型
 */
@FunctionalInterface
public interface QueueConsumer<T> {

    /**
     * 消费消息
     *
     * @param message 队列消息
     * @return 消费结果：true 表示成功，false 表示失败（将触发重试）
     */
    boolean consume(QueueMessage<T> message);

    /**
     * 消费失败时的回调（可选实现）
     *
     * @param message   队列消息
     * @param throwable 异常信息
     */
    default void onError(QueueMessage<T> message, Throwable throwable) {
        // 默认空实现
    }

    /**
     * 消息进入死信队列时的回调（可选实现）
     *
     * @param message 队列消息
     */
    default void onDeadLetter(QueueMessage<T> message) {
        // 默认空实现
    }
}
