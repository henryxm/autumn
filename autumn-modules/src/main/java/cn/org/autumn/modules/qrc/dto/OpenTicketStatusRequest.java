package cn.org.autumn.modules.qrc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenTicketStatusRequest extends OpenTicketClientRequest {
    @Schema(description = "票据uuid")
    private String uuid;
}
