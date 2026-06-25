package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Hook标识", description = "Hook uuid请求")
public class RobotHookUuidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "标识", description = "Hook uuid")
    private String uuid;
}
