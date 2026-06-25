package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "保存用户机器人配额", description = "管理员按用户设置配额，-1继承全局")
public class RobotConfigSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "用户", description = "用户uuid")
    private String uuid;

    @Schema(name = "机器人数", description = "可创建机器人数量，-1继承全局")
    private Integer maxRobots;

    @Schema(name = "令牌数", description = "每机器人最大令牌数，-1继承全局")
    private Integer maxTokens;

    @Schema(name = "Hook数", description = "每机器人最大Hook数，-1继承全局")
    private Integer maxHooks;
}
