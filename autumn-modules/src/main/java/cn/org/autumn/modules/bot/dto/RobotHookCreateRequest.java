package cn.org.autumn.modules.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(name = "创建Hook", description = "创建机器人Hook")
public class RobotHookCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "机器人", description = "机器人uuid")
    private String robot;

    @Schema(name = "名称", description = "Hook名称")
    private String name;

    @Schema(name = "回调", description = "回调URL")
    private String callbackUrl;

    @Schema(name = "密钥", description = "签名校验密钥")
    private String secret;

    @Schema(name = "事件", description = "订阅事件CSV")
    private String events;

    @Schema(name = "描述", description = "用途说明")
    private String description;
}
