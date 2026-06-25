package cn.org.autumn.modules.bot.dto;

import cn.org.autumn.modules.bot.entity.RobotHookEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Hook视图", description = "Hook信息（不含密钥明文）")
public class RobotHookView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String robot;
    private String owner;
    private String name;
    private String callbackUrl;
    private String events;
    private String description;
    private int status;
    private Date createTime;
    private Date updateTime;
    private Date lastInvokeTime;

    public static RobotHookView of(RobotHookEntity hook) {
        if (hook == null)
            return null;
        RobotHookView view = new RobotHookView();
        view.setUuid(hook.getUuid());
        view.setRobot(hook.getRobot());
        view.setOwner(hook.getOwner());
        view.setName(hook.getName());
        view.setCallbackUrl(hook.getCallback());
        view.setEvents(hook.getEvents());
        view.setDescription(hook.getDescription());
        view.setStatus(hook.getStatus());
        view.setCreateTime(hook.getCreateTime());
        view.setUpdateTime(hook.getUpdateTime());
        view.setLastInvokeTime(hook.getLastInvokeTime());
        return view;
    }
}
