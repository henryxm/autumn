package cn.org.autumn.modules.bot.dto;

import cn.org.autumn.jackson.JsonPayloadDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotMessagePushRequest {

    @Schema(description = "消息类型，与 Hook/流程订阅的 event 一致，如 order.paid")
    private String type;

    @Schema(description = "业务载荷 JSON（可传 JSON 对象/数组，或已序列化的 JSON 字符串）")
    @JsonDeserialize(using = JsonPayloadDeserializer.class)
    private String data;

    @Schema(description = "幂等键，可选；重复提交在缓存有效期内返回首次结果")
    private String messageId;
}
