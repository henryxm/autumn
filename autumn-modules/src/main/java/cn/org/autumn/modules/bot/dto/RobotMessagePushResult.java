package cn.org.autumn.modules.bot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotMessagePushResult {

    private String messageId;
    private String type;
    /** 是否已入队（异步分发） */
    private boolean queued;
    /** 是否为幂等重复请求（未再次入队） */
    private boolean duplicate;

    public static RobotMessagePushResult accepted(String messageId, String type) {
        RobotMessagePushResult result = new RobotMessagePushResult();
        result.setMessageId(messageId);
        result.setType(type);
        result.setQueued(true);
        result.setDuplicate(false);
        return result;
    }
}
