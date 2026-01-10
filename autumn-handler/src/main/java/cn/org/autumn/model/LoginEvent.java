package cn.org.autumn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 用户登录事件模型
 * <p>
 * 用于传递用户登录相关的信息，包括用户ID、登录时间、登录来源、设备信息等
 *
 * @author Autumn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private String user;

    /**
     * 登录时间
     */
    @Builder.Default
    private long time = System.currentTimeMillis();

    /**
     * 登录来源（如：socket、web、mobile等）
     */
    private String source;

    /**
     * 登录IP地址
     */
    private String ip;

    /**
     * 设备信息（如：设备ID、设备类型等）
     */
    private String device;

    /**
     * 会话ID（Session ID或Token）
     */
    private String session;

    /**
     * 登录类型（如：normal、auto、refresh等）
     */
    private String type;

    /**
     * 扩展属性，用于传递额外的信息
     */
    private Map<String, Object> attributes;

    /**
     * 是否首次登录
     */
    @Builder.Default
    private Boolean first = false;

    /**
     * 登录渠道（如：im、web、app等）
     */
    private String channel;

    /**
     * 事件ID，用于追踪和去重
     */
    private String id;

    /**
     * 实例ID，用于标识事件来源的实例
     */
    private String instance;
}
