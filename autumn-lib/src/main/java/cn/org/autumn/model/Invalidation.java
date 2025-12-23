package cn.org.autumn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缓存失效消息
 * 用于多实例部署时的缓存一致性通知
 *
 * @author Autumn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invalidation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存键
     */
    private String key;

    /**
     * 操作类型：PUT（更新）、REMOVE（删除）、CLEAR（清空）
     */
    private String operation;

    /**
     * 时间戳（毫秒），用于防止重复处理
     */
    private Long timestamp;

    /**
     * 实例ID（可选），用于标识发送消息的实例，避免自己处理自己的消息
     */
    private String instanceId;

    public Invalidation(String cacheName, String key, String operation) {
        this.cacheName = cacheName;
        this.key = key;
        this.operation = operation;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 操作类型常量
     */
    public static class Operation {
        public static final String PUT = "PUT";
        public static final String REMOVE = "REMOVE";
        public static final String CLEAR = "CLEAR";
    }
}
