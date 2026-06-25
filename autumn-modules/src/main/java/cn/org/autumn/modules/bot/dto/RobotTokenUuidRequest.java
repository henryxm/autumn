package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "令牌标识", description = "令牌uuid请求")
public class RobotTokenUuidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "标识", description = "令牌uuid")
    private String uuid;
}
