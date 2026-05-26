package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "轮换令牌", description = "轮换机器人API令牌")
public class RobotRotateTokenRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "标识", description = "机器人业务uuid", required = true)
    private String uuid;

    @Schema(name = "令牌天数", description = "新令牌有效天数，空则默认365天")
    private Integer tokenExpireDays;
}
