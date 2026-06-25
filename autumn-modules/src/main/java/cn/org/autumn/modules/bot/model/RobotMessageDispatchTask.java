package cn.org.autumn.modules.bot.model;

import cn.org.autumn.model.RobotMessage;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 机器人入站消息队列任务（API 校验入队，消费端再分发给订阅者与 Hook）。
 */
@Getter
@Setter
@NoArgsConstructor
public class RobotMessageDispatchTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private String robot;
    private String owner;
    private String type;
    /** 规范化的 JSON 文本 */
    private String data;
    private long timestamp;

    public RobotMessageDispatchTask(String messageId, String robot, String owner, String type, String data, long timestamp) {
        this.messageId = messageId;
        this.robot = robot;
        this.owner = owner;
        this.type = type;
        this.data = data;
        this.timestamp = timestamp;
    }

    public RobotMessage toMessage() {
        RobotMessage message = new RobotMessage();
        message.setMessageId(messageId);
        message.setRobot(robot);
        message.setOwner(owner);
        message.setType(type);
        message.setData(data);
        message.setTimestamp(timestamp);
        return message;
    }
}
