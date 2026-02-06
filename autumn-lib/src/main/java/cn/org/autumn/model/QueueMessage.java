package cn.org.autumn.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 队列消息包装类
 * 包含消息体和元数据信息
 *
 * @param <T> 消息体类型
 */
@Data
@Builder
public class QueueMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    /**
     * 消息体
     */
    private T body;

    /**
     * 消息创建时间戳（毫秒）
     */
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli();

    /**
     * 延迟执行时间戳（毫秒）
     * 仅对延迟队列有效
     */
    private Long delay;

    /**
     * 消息优先级（数字越小优先级越高）
     * 仅对优先级队列有效
     */
    @Builder.Default
    private int priority = 5;

    /**
     * 重试次数
     */
    @Builder.Default
    private int retry = 0;

    /**
     * 消息头/扩展属性
     */
    private Map<String, String> headers;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 消息追踪ID（用于链路追踪）
     */
    private String trace;

    /**
     * 创建简单消息
     */
    public static <T> QueueMessage<T> of(T body) {
        return QueueMessage.<T>builder().body(body).build();
    }

    /**
     * 创建带优先级的消息
     */
    public static <T> QueueMessage<T> ofPriority(T body, int priority) {
        return QueueMessage.<T>builder().body(body).priority(priority).build();
    }

    /**
     * 创建延迟消息
     *
     * @param body        消息体
     * @param delayMillis 延迟毫秒数
     */
    public static <T> QueueMessage<T> ofDelay(T body, long delayMillis) {
        return QueueMessage.<T>builder()
                .body(body)
                .delay(Instant.now().toEpochMilli() + delayMillis)
                .build();
    }

    /**
     * 创建指定执行时间的消息
     *
     * @param body       消息体
     * @param executeAt  执行时间戳（毫秒）
     */
    public static <T> QueueMessage<T> ofScheduled(T body, long executeAt) {
        return QueueMessage.<T>builder()
                .body(body)
                .delay(executeAt)
                .build();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retry++;
    }

    /**
     * 检查消息是否已到达执行时间
     */
    public boolean isReady() {
        if (delay == null) {
            return true;
        }
        return Instant.now().toEpochMilli() >= delay;
    }

    /**
     * 获取距离执行还需等待的毫秒数
     */
    public long getDelayMillis() {
        if (delay == null) {
            return 0;
        }
        long delay = this.delay - Instant.now().toEpochMilli();
        return Math.max(0, delay);
    }
}
