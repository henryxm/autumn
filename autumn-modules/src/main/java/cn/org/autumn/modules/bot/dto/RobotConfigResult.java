package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "用户机器人配额", description = "用户级配额配置及生效值")
public class RobotConfigResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "用户", description = "用户uuid")
    private String uuid;

    @Schema(name = "机器人数", description = "配置值，-1继承全局")
    private int maxRobots;

    @Schema(name = "令牌数", description = "配置值，-1继承全局")
    private int maxTokens;

    @Schema(name = "Hook数", description = "配置值，-1继承全局")
    private int maxHooks;

    @Schema(name = "已用机器人数", description = "库表机器人数（含停用/删除态行）")
    private int usedRobots;

    @Schema(name = "生效机器人数", description = "合并全局后的机器人上限")
    private int effectiveMaxRobots;

    @Schema(name = "生效令牌", description = "合并全局后每机器人令牌上限")
    private int effectiveMaxTokens;

    @Schema(name = "生效Hook", description = "合并全局后每机器人Hook上限")
    private int effectiveMaxHooks;
}
