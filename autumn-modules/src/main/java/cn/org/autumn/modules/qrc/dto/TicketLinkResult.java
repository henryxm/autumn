package cn.org.autumn.modules.qrc.dto;

import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketLinkResult {
    private String uuid;
    private String intent;
    private String status;

    public static TicketLinkResult from(TicketSnapshot ticket) {
        TicketLinkResult result = new TicketLinkResult();
        result.setUuid(ticket.getUuid());
        result.setIntent(ticket.getIntent());
        result.setStatus(ticket.getStatus());
        return result;
    }
}
