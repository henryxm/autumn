package cn.org.autumn.modules.qrc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenTicketClientRequest {
    @Schema(description = "OAuth client_id")
    private String clientId;
    @Schema(description = "OAuth client_secret")
    private String clientSecret;
}
