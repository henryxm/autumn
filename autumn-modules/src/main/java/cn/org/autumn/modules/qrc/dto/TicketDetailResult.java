package cn.org.autumn.modules.qrc.dto;

import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
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
    private String intentTitle;
    private String intentHint;
    private String deviceHint;

    public static TicketDetailResult from(TicketSnapshot ticket, String clientName, String clientIconUri, List<String> scopeLabels, String intentTitle, String intentHint, String deviceHint) {
        TicketDetailResult result = new TicketDetailResult();
        result.setUuid(ticket.getUuid());
        result.setIntent(ticket.getIntent());
        result.setStatus(ticket.getStatus());
        if (ticket.getPayload() != null) {
            result.setPayload(ticket.getPayload());
        }
        result.setClientId(TicketPayloads.get(ticket, "clientId"));
        result.setScope(TicketPayloads.get(ticket, "scope"));
        result.setRedirectUri(TicketPayloads.get(ticket, "redirectUri"));
        result.setClientName(clientName);
        result.setClientIconUri(clientIconUri);
        if (scopeLabels != null && !scopeLabels.isEmpty()) {
            result.setScopeLabels(scopeLabels);
        }
        result.setIntentTitle(intentTitle);
        result.setIntentHint(intentHint);
        result.setDeviceHint(deviceHint);
        return result;
    }
}
