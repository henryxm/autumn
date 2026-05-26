package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 机器人入站消息（外部推送经 API 进入系统后的统一载荷）。
 */
@Getter
@Setter
public class RobotMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息唯一标识 */
    private String messageId;
    /** 机器人 uuid */
    private String robot;
    /** 数据归属用户 uuid */
    private String owner;
    /** 消息类型（与 Hook / 流程订阅的 event 一致） */
    private String type;
    /** 业务载荷（规范化后的 JSON 文本，由调用方按需解析） */
    private String data;
    /** 接收时间毫秒时间戳 */
    private long timestamp;
}
