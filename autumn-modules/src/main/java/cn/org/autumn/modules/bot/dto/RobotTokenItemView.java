package cn.org.autumn.modules.bot.dto;

import cn.org.autumn.modules.bot.entity.RobotTokenEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Schema(name = "令牌条目", description = "机器人API令牌元数据（不含明文与哈希）")
public class RobotTokenItemView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String robot;
    private String tokenPrefix;
    private int status;
    private Date expireTime;
    private Date updateTime;
    private Date lastUsedTime;

    public static RobotTokenItemView of(RobotTokenEntity token) {
        if (token == null)
            return null;
        RobotTokenItemView view = new RobotTokenItemView();
        view.setUuid(token.getUuid());
        view.setRobot(token.getRobot());
        view.setTokenPrefix(token.getTokenPrefix());
        view.setStatus(token.getStatus());
        view.setExpireTime(token.getExpireTime());
        view.setUpdateTime(token.getUpdateTime());
        view.setLastUsedTime(token.getLastUsedTime());
        return view;
    }
}
