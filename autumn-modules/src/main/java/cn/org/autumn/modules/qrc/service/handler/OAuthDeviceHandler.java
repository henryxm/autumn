package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.model.UserContext;
import org.apache.commons.lang.StringUtils;
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
    private SysUserService sysUserService;

    @Override
    public String intent() {
        return Intent.OAUTH_DEVICE;
    }

    @Override
    public void onCreate(TicketSnapshot ticket, CreateContext ctx) throws Exception {
        String clientId = ticket.getPayload().get("clientId");
        clientGrantService.requireTrustedClient(clientId);
        if (ctx != null && StringUtils.isNotBlank(ctx.getClientSecret())) {
            clientGrantService.validateClientSecret(clientId, ctx.getClientSecret());
        }
        String redirectUri = ticket.getPayload().get("redirectUri");
        if (StringUtils.isNotBlank(redirectUri)) {
            clientGrantService.validateRedirectUri(clientGrantService.requireTrustedClient(clientId), redirectUri);
        }
        ClientGrantEntity grant = clientGrantService.getOrDefault(clientId);
        if (!grant.isEnabled()) {
            throw new CodeException("客户端未启用扫码授权", 8630);
        }
        if (StringUtils.isNotBlank(grant.getDelivery())) {
            ticket.getPayload().put("delivery", grant.getDelivery());
        }
    }

    @Override
    public void onScan(TicketSnapshot ticket, UserContext scanner) {
    }

    @Override
    public ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception {
        SysUserEntity user = sysUserService.getByUuid(scanner.getUuid());
        if (user == null || user.getStatus() < 1) {
            throw new CodeException("用户不可用", 8623);
        }
        ClientGrantEntity grant = clientGrantService.getOrDefault(ticket.getPayload().get("clientId"));
        ConfirmResult result = clientGrantService.deliverOAuth(ticket, grant, user);
        result.setCompleted(true);
        return result;
    }
}
