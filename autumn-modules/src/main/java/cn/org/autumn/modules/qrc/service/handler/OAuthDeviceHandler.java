package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.model.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class OAuthDeviceHandler implements IntentHandler {

    @Autowired
    @Lazy
    private ClientGrantService clientGrantService;

    @Autowired
    @Lazy
    private ScanTicketService scanTicketService;

    @Override
    public String intent() {
        return Intent.OAUTH_DEVICE;
    }

    @Override
    public void onCreate(TicketSnapshot ticket, CreateContext ctx) throws Exception {
        String clientId = TicketPayloads.get(ticket, "clientId");
        ClientDetailsEntity client = clientGrantService.requireTrustedClient(clientId);
        if (ctx != null && StringUtils.isNotBlank(ctx.getClientSecret())) {
            clientGrantService.validateClientSecret(clientId, ctx.getClientSecret());
        }
        String redirectUri = TicketPayloads.get(ticket, "redirectUri");
        if (StringUtils.isNotBlank(redirectUri)) {
            clientGrantService.validateRedirectUri(client, redirectUri);
        }
        ClientGrantEntity grant = clientGrantService.requireEnabledGrant(clientId);
        if (StringUtils.isNotBlank(grant.getDelivery())) {
            ticket.getPayload().put("delivery", grant.getDelivery());
        }
    }

    @Override
    public void onScan(TicketSnapshot ticket, UserContext scanner) {
    }

    @Override
    public ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception {
        SysUserEntity user = scanTicketService.requireActiveUser(scanner == null ? null : scanner.getUuid());
        ClientGrantEntity grant = clientGrantService.getOrDefault(TicketPayloads.get(ticket, "clientId"));
        return clientGrantService.deliverOAuth(ticket, grant, user);
    }
}
