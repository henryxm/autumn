package cn.org.autumn.modules.qrc.dto;

import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

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
    private ScannerBrief scannerBrief;

    public static TicketStatusResult from(TicketSnapshot ticket) {
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
        String redirectUri = TicketPayloads.get(ticket, "redirectUri");
        if (StringUtils.isNotBlank(redirectUri)) {
            result.getResult().put("redirectUri", redirectUri);
        }
        return result;
    }
}
