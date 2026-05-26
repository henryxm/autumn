package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Schema(name = "Hook列表", description = "机器人Hook列表")
public class RobotHookListResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "列表", description = "Hook列表")
    private List<RobotHookView> hooks;

    public static RobotHookListResult of(List<RobotHookView> hooks) {
        RobotHookListResult result = new RobotHookListResult();
        result.setHooks(hooks);
        return result;
    }
}
