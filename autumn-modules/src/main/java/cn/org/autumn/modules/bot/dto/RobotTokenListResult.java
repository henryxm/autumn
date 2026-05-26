package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Schema(name = "令牌列表", description = "机器人令牌列表及配额占用")
public class RobotTokenListResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "列表", description = "当前有效令牌")
    private List<RobotTokenItemView> tokens;

    @Schema(name = "已用行数", description = "库表记录数（含已作废），用于配额")
    private int usedRows;

    @Schema(name = "上限", description = "该机器人令牌配额上限")
    private int maxRows;

    public static RobotTokenListResult of(List<RobotTokenItemView> tokens) {
        RobotTokenListResult result = new RobotTokenListResult();
        result.setTokens(tokens);
        return result;
    }
}
