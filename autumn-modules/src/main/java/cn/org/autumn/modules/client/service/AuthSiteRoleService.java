package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthSiteConfig;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 站点 AS/RP 角色与 {@code LOGIN_AUTHENTICATION} 所指的 RP 客户端解析。 */
@Service
public class AuthSiteRoleService {

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    WebAuthenticationService webAuthenticationService;

    public AuthSiteConfig getSiteConfig() {
        return sysConfigService.getAuthSiteConfig();
    }

    public boolean isRpEnabled() {
        String role = getSiteConfig().normalizedSiteRole();
        return AuthSiteConfig.ROLE_RP_ONLY.equals(role) || AuthSiteConfig.ROLE_AS_AND_RP.equals(role);
    }

    public String resolveQrcWebMode() {
        AuthSiteConfig config = getSiteConfig();
        String mode = config.normalizedQrcWebMode();
        if ("as".equals(mode)) {
            return "as";
        }
        if ("rp".equals(mode)) {
            return "rp";
        }
        if (AuthSiteConfig.ROLE_RP_ONLY.equals(config.normalizedSiteRole())) {
            return "rp";
        }
        return "as";
    }

    /** 请求参数 client_id 优先，否则 LOGIN_AUTHENTICATION。 */
    public String resolveRpClientId(HttpServletRequest request) {
        if (!isRpEnabled()) {
            return null;
        }
        String clientId = resolveClientIdParam(request);
        if (StringUtils.isBlank(clientId)) {
            clientId = request == null ? null : sysConfigService.getOauth2LoginClientId(request.getHeader("host"));
        }
        if (StringUtils.isBlank(clientId)) {
            clientId = sysConfigService.getOauth2LoginClientId();
        }
        return StringUtils.isBlank(clientId) ? null : clientId.trim();
    }

    /** 仅解析请求中的 client_id / clientId，不含 {@code LOGIN_AUTHENTICATION} 回退（用于 {@code /oauth2/login} 入口）。 */
    public String resolveExplicitRpClientId(HttpServletRequest request) {
        if (!isRpEnabled() || request == null) {
            return null;
        }
        String clientId = resolveClientIdParam(request);
        return StringUtils.isBlank(clientId) ? null : clientId.trim();
    }

    public String resolveClientIdParam(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String clientId = request.getParameter("client_id");
        if (StringUtils.isBlank(clientId)) {
            clientId = request.getParameter("clientId");
        }
        return StringUtils.isBlank(clientId) ? null : clientId.trim();
    }

    public WebAuthenticationEntity resolveRpClient(HttpServletRequest request) {
        if (!isRpEnabled()) {
            return null;
        }
        String clientId = resolveRpClientId(request);
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        return webAuthenticationService.getByClientId(clientId);
    }
}
