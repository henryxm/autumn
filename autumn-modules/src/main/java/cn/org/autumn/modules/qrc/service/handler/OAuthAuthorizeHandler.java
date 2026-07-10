package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.auth.scope.AuthScopeResolution;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
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
public class OAuthAuthorizeHandler implements IntentHandler {

    @Autowired
    @Lazy
    private ClientGrantService clientGrantService;

    @Autowired
    @Lazy
    private ScanTicketService scanTicketService;

    @Autowired
    private AuthScopeSupport authScopeSupport;

    @Override
    public String intent() {
        return Intent.OAUTH_AUTHORIZE;
    }

    @Override
    public void onCreate(TicketSnapshot ticket, CreateContext ctx) throws Exception {
        validateOAuthPayload(ticket);
    }

    @Override
    public void onScan(TicketSnapshot ticket, UserContext scanner) {
    }

    @Override
    public ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception {
        SysUserEntity user = requireUser(scanner);
        String exchangeToken = scanTicketService.createExchangeToken(user.getUuid(), ticket.getUuid());
        return ConfirmResult.ofExchange(exchangeToken);
    }

    protected void validateOAuthPayload(TicketSnapshot ticket) throws CodeException {
        String clientId = TicketPayloads.get(ticket, "clientId");
        String redirectUri = TicketPayloads.get(ticket, "redirectUri");
        ClientDetailsEntity client = clientGrantService.requireTrustedClient(clientId);
        clientGrantService.validateRedirectUri(client, redirectUri);
        clientGrantService.requireEnabledGrant(clientId);
        validateTicketScope(ticket, client);
    }

    private void validateTicketScope(TicketSnapshot ticket, ClientDetailsEntity client) throws CodeException {
        ClientGrantEntity grant = clientGrantService.getOrDefault(TicketPayloads.get(ticket, "clientId"));
        String requested = TicketPayloads.get(ticket, "scope");
        if (grant == null || StringUtils.isBlank(grant.getScopes())) {
            if (StringUtils.isNotBlank(requested)) {
                throw new CodeException("授权范围无效", 8610);
            }
            return;
        }
        ClientDetailsEntity allowed = new ClientDetailsEntity();
        allowed.setScope(grant.getScopes());
        AuthScopeResolution resolution = authScopeSupport.resolveBoundedQrc(allowed.scopes(), requested, AuthTrack.OAUTH);
        if (resolution.getGranted().isEmpty()) {
            throw new CodeException("授权范围无效", 8610);
        }
    }

    public ClientGrantEntity loadGrant(TicketSnapshot ticket) {
        return clientGrantService.getOrDefault(TicketPayloads.get(ticket, "clientId"));
    }

    public SysUserEntity requireUser(UserContext scanner) throws CodeException {
        return scanTicketService.requireActiveUser(scanner == null ? null : scanner.getUuid());
    }
}
