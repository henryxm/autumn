package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.model.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class OAuthConsentHandler implements IntentHandler {

    @Autowired
    @Lazy
    private OAuthAuthorizeHandler oauthAuthorizeHandler;

    @Autowired
    @Lazy
    private ClientGrantService clientGrantService;

    @Override
    public String intent() {
        return Intent.OAUTH_CONSENT;
    }

    @Override
    public void onCreate(TicketSnapshot ticket, CreateContext ctx) throws Exception {
        oauthAuthorizeHandler.onCreate(ticket, ctx);
    }

    @Override
    public void onScan(TicketSnapshot ticket, UserContext scanner) {
        oauthAuthorizeHandler.onScan(ticket, scanner);
    }

    @Override
    public ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception {
        SysUserEntity user = oauthAuthorizeHandler.requireUser(scanner);
        ClientGrantEntity grant = oauthAuthorizeHandler.loadGrant(ticket);
        ConfirmResult result = clientGrantService.deliverOAuth(ticket, grant, user);
        String redirectUri = ticket.getPayload().get("redirectUri");
        String state = ticket.getPayload().get("state");
        String callback = ticket.getPayload().get("callback");
        String code = result.getResult().get("code");
        if (StringUtils.isNotBlank(code) && StringUtils.isNotBlank(redirectUri)) {
            result.setRedirect(clientGrantService.buildAuthorizeRedirect(redirectUri, code, state, callback));
            result.setCompleted(true);
            result.getResult().put("redirectUri", redirectUri);
        }
        return result;
    }
}
