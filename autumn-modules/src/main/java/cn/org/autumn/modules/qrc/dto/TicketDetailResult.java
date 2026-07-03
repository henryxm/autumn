package cn.org.autumn.modules.qrc.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketDetailResult {
    private String uuid;
    private String intent;
    private String status;
    private String clientId;
    private String clientName;
    private String clientIconUri;
    private String scope;
    private String redirectUri;
    private Map<String, String> payload;
    private List<String> scopeLabels;

    public static TicketDetailResult from(cn.org.autumn.modules.qrc.model.TicketSnapshot ticket, String clientName, String clientIconUri, List<String> scopeLabels) {
        TicketDetailResult result = new TicketDetailResult();
        result.setUuid(ticket.getUuid());
        result.setIntent(ticket.getIntent());
        result.setStatus(ticket.getStatus());
        if (ticket.getPayload() != null) {
            result.setPayload(ticket.getPayload());
            result.setClientId(ticket.getPayload().get("clientId"));
            result.setScope(ticket.getPayload().get("scope"));
            result.setRedirectUri(ticket.getPayload().get("redirectUri"));
        }
        result.setClientName(clientName);
        result.setClientIconUri(clientIconUri);
        if (scopeLabels != null && !scopeLabels.isEmpty()) {
            result.setScopeLabels(scopeLabels);
        }
        return result;
    }
}
