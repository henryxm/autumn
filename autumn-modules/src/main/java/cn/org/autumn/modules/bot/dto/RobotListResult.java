package cn.org.autumn.modules.bot.dto;

import cn.org.autumn.modules.sys.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Schema(name = "机器人列表", description = "当前用户名下机器人")
public class RobotListResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "列表", description = "机器人账号列表")
    private List<User> list;

    public static RobotListResult of(List<User> list) {
        RobotListResult result = new RobotListResult();
        result.setList(list);
        return result;
    }
}
