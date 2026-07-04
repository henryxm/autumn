package cn.org.autumn.modules.qrc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenTicketCreateRequest extends OpenTicketClientRequest {
    @Schema(description = "redirect_uri")
    private String redirectUri;
    @Schema(description = "scope")
    private String scope;
    @Schema(description = "state")
    private String state;
    @Schema(description = "扩展载荷")
    private Map<String, String> payload = new HashMap<>();
}
