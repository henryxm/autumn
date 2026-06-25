package cn.org.autumn.modules.bot.shiro;

import cn.org.autumn.cluster.UserMapping;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import java.io.Serializable;
import lombok.Getter;

@Getter
public class RobotPrincipal implements Serializable, UserMapping {
    private static final long serialVersionUID = 1L;

    private final String uuid;
    private final String owner;
    private final String name;
    private final String icon;
    private final int status;

    public RobotPrincipal(RobotEntity robot) {
        this.uuid = robot.getUuid();
        this.owner = robot.getOwner();
        this.name = robot.getNickname();
        this.icon = robot.getIcon();
        this.status = robot.getStatus();
    }

    @Override
    public String getUsername() {
        return "robot:" + uuid;
    }

    @Override
    public boolean isRobot() {
        return true;
    }
}
