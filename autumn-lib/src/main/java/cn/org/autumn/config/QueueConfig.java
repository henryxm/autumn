package cn.org.autumn.config;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * 队列配置类
 * 用于定义队列的元数据信息
 */
@Getter
@Builder
public class QueueConfig {

    /**
     * 队列名称
     */
    private final String name;

    /**
     * 消息类型
     */
    private final Class<?> type;

    /**
     * 队列类型
     */
    @Builder.Default
    private final QueueType queueType = QueueType.MEMORY;

    /**
     * 队列最大容量（仅对内存队列有效）
     * 0 表示无限制
     */
    @Builder.Default
    private final int capacity = 100000;

    /**
     * 消费超时时间
     */
    @Builder.Default
    private final long timeout = 30;

    /**
     * 超时时间单位
     */
    @Builder.Default
    private final TimeUnit unit = TimeUnit.SECONDS;

    /**
     * 是否启用持久化（仅对Redis队列有效）
     */
    @Builder.Default
    private final boolean persistent = true;

    /**
     * 消息过期时间（秒），0表示永不过期
     */
    @Builder.Default
    private final long expire = 0;

    /**
     * 消费者组名称（仅对Redis Stream有效）
     */
    private final String group;

    /**
     * 消费者名称（仅对Redis Stream有效）
     */
    private final String consumer;

    /**
     * 最大重试次数
     */
    @Builder.Default
    private final int retries = 3;

    /**
     * 是否启用死信队列
     */
    @Builder.Default
    private final boolean deadLetter = false;

    /**
     * 死信队列名称
     */
    private final String deadLetterQueue;

    /**
     * 是否启用自动启动消费者
     * 当发送消息时，如果消费者未运行，自动启动消费者
     */
    @Builder.Default
    private final boolean auto = true;

    /**
     * 空闲超时时间（配合 autoStop 使用）
     * 当消费者在此时间内没有处理任何消息，将自动停止
     */
    @Builder.Default
    private final long idleTime = 60;

    /**
     * 空闲超时时间单位
     */
    @Builder.Default
    private final TimeUnit idleUnit = TimeUnit.SECONDS;

    /**
     * 自动启动时的并发消费者数量
     */
    @Builder.Default
    private final int concurrency = 1;

    /**
     * 队列类型枚举
     */
    public enum QueueType {
        /**
         * 内存队列（基于BlockingQueue）
         * 适用于单机场景，高性能，重启后数据丢失
         */
        MEMORY,

        /**
         * Redis List队列
         * 适用于分布式场景，简单队列，支持持久化
         */
        REDIS_LIST,

        /**
         * Redis Stream队列
         * 适用于分布式场景，支持消费者组、消息确认、持久化
         */
        REDIS_STREAM,

        /**
         * 延迟队列（基于Redis Sorted Set）
         * 适用于需要延迟处理的场景
         */
        DELAY,

        /**
         * 优先级队列（内存）
         * 支持按优先级消费消息
         */
        PRIORITY
    }

    /**
     * 验证必填字段
     */
    public void validate() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Queue name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Message type is required");
        }
        if (queueType == QueueType.REDIS_STREAM && (group == null || group.isEmpty())) {
            throw new IllegalArgumentException("Consumer group is required for Redis Stream queue");
        }
    }

    /**
     * 获取死信队列名称，如果未设置则使用默认名称
     */
    public String getDeadLetterQueueName() {
        if (deadLetterQueue != null && !deadLetterQueue.isEmpty()) {
            return deadLetterQueue;
        }
        return name + ":dead";
    }
}
