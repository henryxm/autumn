package cn.org.autumn.modules.qrc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketCreateResult {
    private String uuid;
    private String qrUrl;
    private long expireIn;
    private String intent;
    private String status;

    public static TicketCreateResult of(String uuid, String qrUrl, long expireIn, String intent, String status) {
        TicketCreateResult result = new TicketCreateResult();
        result.setUuid(uuid);
        result.setQrUrl(qrUrl);
        result.setExpireIn(expireIn);
        result.setIntent(intent);
        result.setStatus(status);
        return result;
    }
}
