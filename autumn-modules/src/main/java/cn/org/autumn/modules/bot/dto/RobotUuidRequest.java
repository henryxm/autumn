package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "机器人标识", description = "指定机器人uuid")
public class RobotUuidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "标识", description = "机器人业务uuid", required = true)
    private String uuid;
}
