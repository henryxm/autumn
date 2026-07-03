package cn.org.autumn.modules.qrc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketCreateRequest {
    @Schema(description = "意图:SELF_WEB_LOGIN/OAUTH_DEVICE等")
    private String intent;
    @Schema(description = "扩展载荷")
    private Map<String, String> payload = new HashMap<>();
}
