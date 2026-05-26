package cn.org.autumn.modules.bot.dto;

import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.sys.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotCreateResult {
    private User robot;
    private String token;

    public static RobotCreateResult of(User robot, String token) {
        RobotCreateResult result = new RobotCreateResult();
        result.setRobot(robot);
        result.setToken(token);
        return result;
    }

    public static RobotCreateResult of(RobotEntity entity, User user, String token) {
        return of(user, token);
    }
}
