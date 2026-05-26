package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "更新机器人", description = "更新机器人资料与 IM 访问策略")
public class RobotUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "机器人uuid", description = "机器人业务主键")
    private String uuid;

    @Schema(name = "名称", description = "展示名称")
    private String name;

    @Schema(name = "描述", description = "用途说明")
    private String description;

    @Schema(name = "头像", description = "图标地址")
    private String icon;

    @Schema(name = "访问模式", description = "private/public/subscribe")
    private String access;

    @Schema(name = "拉黑", description = "平台级封禁")
    private Boolean black;
}
