package cn.org.autumn.modules.bot.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RobotHookDispatchTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String user;
    private String robot;
    private String hook;
    private String event;
    private String timestamp;
    /** 业务载荷 JSON 文本 */
    private String data;

    public RobotHookDispatchTask(String user, String robot, String hook, String event, String timestamp, String data) {
        this.user = user;
        this.robot = robot;
        this.hook = hook;
        this.event = event;
        this.timestamp = timestamp;
        this.data = data;
    }
}
