package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "机器人令牌", description = "明文令牌仅返回一次")
public class RobotTokenResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "令牌", description = "rbt_ 前缀API访问令牌")
    private String token;

    public static RobotTokenResult of(String token) {
        RobotTokenResult result = new RobotTokenResult();
        result.setToken(token);
        return result;
    }
}
