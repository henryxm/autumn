package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

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

    @Schema(name = "已用机器人数", description = "占用配额的机器人数（含停用，不含软删/销毁）")
    private int usedRobots;

    @Schema(name = "待清理软删数", description = "status=-1 且尚未硬销毁的机器人数")
    private int pendingSoftDeleted;

    @Schema(name = "软删门禁", description = "为 true 时暂不可创建机器人（软删数超过全局 maxSoftDeletePending）")
    private boolean softDeleteCreateBlocked;

    @Schema(name = "生效软删门禁上限", description = "全局 RobotQuotaConfig.maxSoftDeletePending")
    private int effectiveMaxSoftDeletePending;

    @Schema(name = "生效软删保留天数", description = "全局 RobotQuotaConfig.deletedRetentionDays")
    private int effectiveDeletedRetentionDays;

    @Schema(name = "生效机器人数", description = "合并全局后的机器人上限")
    private int effectiveMaxRobots;

    @Schema(name = "生效令牌", description = "合并全局后每机器人令牌上限")
    private int effectiveMaxTokens;

    @Schema(name = "生效Hook", description = "合并全局后每机器人Hook上限")
    private int effectiveMaxHooks;
}
