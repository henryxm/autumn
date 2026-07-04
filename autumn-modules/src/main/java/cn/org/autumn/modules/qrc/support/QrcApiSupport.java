package cn.org.autumn.modules.qrc.support;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.dto.OpenTicketClientRequest;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.TicketDetailResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.qrc.service.ConsentSupport;
import cn.org.autumn.modules.qrc.service.IntentDisplaySupport;
import cn.org.autumn.utils.IPUtils;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QrcApiSupport {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private ClientGrantService clientGrantService;

    @Autowired
    private ConsentSupport consentSupport;

    @Autowired
    private IntentDisplaySupport intentDisplaySupport;

    public <T extends OpenTicketClientRequest> T requireOpenClient(T data) throws CodeException {
        if (data == null || StringUtils.isBlank(data.getClientId()) || StringUtils.isBlank(data.getClientSecret())) {
            throw new CodeException("client_id与client_secret不能为空", 8609);
        }
        clientGrantService.validateClientSecret(data.getClientId(), data.getClientSecret());
        return data;
    }

    public void assertTicketClient(TicketSnapshot ticket, String clientId) throws CodeException {
        String ticketClient = TicketPayloads.get(ticket, "clientId");
        if (StringUtils.isNotBlank(ticketClient) && !clientId.equals(ticketClient)) {
            throw new CodeException("无权查询该票据", 8619);
        }
    }

    public CreateContext buildOpenDeviceContext(OpenTicketCreateRequest data, HttpServletRequest servlet) {
        Map<String, String> payload = new HashMap<>();
        payload.put("clientId", data.getClientId());
        if (StringUtils.isNotBlank(data.getRedirectUri())) {
            payload.put("redirectUri", data.getRedirectUri());
        }
        if (StringUtils.isNotBlank(data.getScope())) {
            payload.put("scope", data.getScope());
        }
        if (StringUtils.isNotBlank(data.getState())) {
            payload.put("state", data.getState());
        }
        if (data.getPayload() != null) {
            payload.putAll(data.getPayload());
        }
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.OAUTH_DEVICE);
        ctx.setClientId(data.getClientId());
        ctx.setClientSecret(data.getClientSecret());
        ctx.setPayload(payload);
        ctx.setIp(IPUtils.getIp(servlet));
        ctx.setAgent(servlet.getHeader("user-agent"));
        return ctx;
    }

    public TicketDetailResult buildDetail(TicketSnapshot ticket) {
        String clientId = TicketPayloads.get(ticket, "clientId");
        String clientName = "";
        String clientIcon = "";
        if (StringUtils.isNotBlank(clientId)) {
            ClientDetailsEntity client = clientDetailsService.findByClientId(clientId);
            if (client != null) {
                clientName = client.getClientName();
                clientIcon = client.getClientIconUri();
            }
        }
        return TicketDetailResult.from(ticket, clientName, clientIcon, consentSupport.describeScopes(ticket), intentDisplaySupport.intentTitle(ticket, clientName), intentDisplaySupport.intentHint(ticket), intentDisplaySupport.deviceHint(ticket));
    }
}
