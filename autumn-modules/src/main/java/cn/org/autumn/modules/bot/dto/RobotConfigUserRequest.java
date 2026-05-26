package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "用户配额查询", description = "按用户uuid查询配额，空则查当前用户")
public class RobotConfigUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "用户", description = "用户uuid，空表示当前登录用户")
    private String uuid;
}
