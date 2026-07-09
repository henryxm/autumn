package cn.org.autumn.modules.qrc.support;

import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.oauth.oauth2.support.AuthAuthorizeLoginSupport;
import cn.org.autumn.modules.qrc.dto.SessionExchangeRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateRequest;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.qrc.shiro.ScanLoginToken;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.controller.SysAuthSupport;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.IPUtils;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScanWebSupport {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ShiroSessionService shiroSessionService;

    @Autowired
    private SuperPositionModelService superPositionModelService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ScanTicketService scanTicketService;

    public CreateContext buildWebCreateContext(TicketCreateRequest data, HttpServletRequest servlet) {
        String intent = data == null || StringUtils.isBlank(data.getIntent()) ? Intent.SELF_WEB_LOGIN : data.getIntent();
        CreateContext ctx = new CreateContext();
        ctx.setIntent(intent);
        ctx.setIp(IPUtils.getIp(servlet));
        ctx.setAgent(servlet.getHeader("user-agent"));
        if (data != null && data.getPayload() != null) {
            ctx.setPayload(new HashMap<>(data.getPayload()));
        }
        return ctx;
    }

    public String exchangeSession(SessionExchangeRequest data, HttpServletRequest servlet) {
        String exchange = data == null ? null : data.getExchange();
        if (StringUtils.isBlank(exchange)) {
            throw new IllegalArgumentException("exchange不能为空");
        }
        boolean rememberMe = data != null && data.isRememberMe();
        String intent = scanTicketService.peekExchangeTicketIntent(exchange);
        CrudGuard.force(() -> {
            ScanLoginToken token = new ScanLoginToken(exchange);
            token.setRememberMe(rememberMe);
            sysUserService.login(token);
            try {
                SysUserEntity current = ShiroUtils.getUserEntity();
                if (current != null && StringUtils.isNotBlank(current.getUuid())) {
                    shiroSessionService.clearForceLogout(current.getUuid());
                    userProfileService.updateLoginIp(current.getUuid(), IPUtils.getIp(servlet), servlet.getHeader("user-agent"));
                }
            } catch (Exception ignored) {
            }
        });
        String redirect = SysAuthSupport.resolvePostLoginRedirect(servlet, superPositionModelService, sysConfigService.getAccountAuthConfig());
        if (Intent.OAUTH_AUTHORIZE.equals(intent)
                || AuthAuthorizeLoginSupport.isAuthorizeCallback(servlet != null ? servlet.getParameter("callback") : null)
                || AuthAuthorizeLoginSupport.isAuthorizeCallback(redirect)) {
            AuthAuthorizeLoginSupport.saveLoginTab(servlet, AuthAuthorizeLoginSupport.TAB_QR);
            redirect = AuthAuthorizeLoginSupport.appendLoginTab(redirect, AuthAuthorizeLoginSupport.TAB_QR);
        }
        return redirect;
    }
}
