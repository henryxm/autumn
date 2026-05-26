package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "创建机器人", description = "创建机器人请求体")
public class RobotCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "名称", description = "机器人展示名称")
    private String name;

    @Schema(name = "描述", description = "用途说明")
    private String description;

    @Schema(name = "头像", description = "图标地址")
    private String icon;

    @Schema(name = "令牌天数", description = "API令牌有效天数，空则默认365天")
    private Integer tokenExpireDays;

    @Schema(name = "访问模式", description = "private仅主人;public任意IM用户;subscribe需订阅")
    private String access;
}
