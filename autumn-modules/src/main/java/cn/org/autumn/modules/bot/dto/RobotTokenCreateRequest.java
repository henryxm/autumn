package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "创建令牌", description = "在配额内为机器人新增API令牌")
public class RobotTokenCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "标识", description = "机器人业务uuid", required = true)
    private String uuid;

    @Schema(name = "令牌天数", description = "新令牌有效天数，空则默认365天")
    private Integer tokenExpireDays;
}
