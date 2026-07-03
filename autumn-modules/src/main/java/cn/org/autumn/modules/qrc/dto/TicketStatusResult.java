package cn.org.autumn.modules.qrc.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketStatusResult {
    private String uuid;
    private String status;
    private String intent;
    private String exchange;
    private String redirect;
    private Map<String, String> result = new HashMap<>();
    private long expireIn;

    public static TicketStatusResult from(cn.org.autumn.modules.qrc.model.TicketSnapshot ticket) {
        TicketStatusResult result = new TicketStatusResult();
        result.setUuid(ticket.getUuid());
        result.setStatus(ticket.getStatus());
        result.setIntent(ticket.getIntent());
        result.setExchange(ticket.getExchange());
        result.setRedirect(ticket.getRedirect());
        if (ticket.getResult() != null) {
            result.setResult(new HashMap<>(ticket.getResult()));
        }
        result.setExpireIn(Math.max(0, (ticket.getExpired() - System.currentTimeMillis()) / 1000));
        if (ticket.getPayload() != null && ticket.getPayload().containsKey("redirectUri")) {
            result.getResult().put("redirectUri", ticket.getPayload().get("redirectUri"));
        }
        return result;
    }
}
