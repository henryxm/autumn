package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.modules.client.dto.ScanLoginCredentialView;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.model.ScanLoginCredentialContext;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.WebPathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScanLoginCredentialService {

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private WebOauthEndpointResolver webOauthEndpointResolver;

    @Autowired
    private SysConfigService sysConfigService;

    public ScanLoginCredentialContext require(String type, String id) {
        if (StringUtils.isBlank(type) || StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("type 与 id 不能为空");
        }
        String normalized = type.trim().toLowerCase();
        if (AuthLoginProviderType.OAUTH2_CLASSIC.equals(normalized)) {
            return requireClassic(id.trim());
        }
        if (AuthLoginProviderType.OAUTH2_OPEN.equals(normalized)) {
            return requireOpen(id.trim());
        }
        throw new IllegalArgumentException("不支持的凭证类型: " + type);
    }

    public ScanLoginCredentialView toView(ScanLoginCredentialContext ctx) {
        if (ctx == null) {
            return null;
        }
        ScanLoginCredentialView view = new ScanLoginCredentialView();
        view.setType(ctx.getType());
        view.setId(ctx.getId());
        view.setName(ctx.getName());
        view.setRedirectUri(ctx.getRedirectUri());
        view.setOriginUri(ctx.getOriginUri());
        view.setPlatformBaseUrl(ctx.getPlatformBaseUrl());
        view.setQrcMode(ctx.getQrcMode());
        String baseUrl = sysConfigService.getBaseUrl();
        view.setInboundCallbackUri(joinBasePath(baseUrl, "/client/oauth2/qrc/web/inbound"));
        if (AuthLoginProviderType.OAUTH2_CLASSIC.equals(ctx.getType()) && ctx.getWebAuth() != null) {
            if (webOauthEndpointResolver.hasRemoteOrigin(ctx.getWebAuth())) {
                view.setQrcOpenCreateUri(webOauthEndpointResolver.resolveQrcOpenCreateUri(ctx.getWebAuth()));
            }
        } else if (AuthLoginProviderType.OAUTH2_OPEN.equals(ctx.getType()) && StringUtils.isNotBlank(ctx.getPlatformBaseUrl())) {
            view.setQrcOpenCreateUri(normalizeOrigin(ctx.getPlatformBaseUrl()) + WebOauthEndpointResolver.PATH_QRC_OPEN_CREATE);
        }
        return view;
    }

    private ScanLoginCredentialContext requireClassic(String clientId) {
        WebAuthenticationEntity web = webAuthenticationService.getByClientId(clientId);
        if (web == null) {
            throw new IllegalArgumentException("未找到经典 OAuth 客户端: " + clientId);
        }
        if (StringUtils.isBlank(web.getClientSecret()) || StringUtils.isBlank(web.getRedirectUri())) {
            throw new IllegalArgumentException("客户端凭证或回调未配置完整");
        }
        ScanLoginCredentialContext ctx = new ScanLoginCredentialContext();
        ctx.setType(AuthLoginProviderType.OAUTH2_CLASSIC);
        ctx.setId(clientId);
        ctx.setName(StringUtils.defaultIfBlank(web.getName(), clientId));
        ctx.setClientId(web.getClientId());
        ctx.setClientSecret(web.getClientSecret());
        ctx.setRedirectUri(web.getRedirectUri());
        ctx.setScope(StringUtils.defaultIfBlank(web.getScope(), "basic"));
        ctx.setOriginUri(webOauthEndpointResolver.resolveOriginUri(web));
        ctx.setQrcMode(webOauthEndpointResolver.hasRemoteOrigin(web) ? "rp" : "as");
        ctx.setWebAuth(web);
        return ctx;
    }

    private ScanLoginCredentialContext requireOpen(String appId) {
        ConnectAppEntity app = connectAppService.getByAppId(appId);
        if (app == null || app.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
            throw new IllegalArgumentException("未找到有效的开放平台应用: " + appId);
        }
        if (StringUtils.isBlank(app.getRedirectUri()) || !connectAppService.hasConfiguredSecret(app)) {
            throw new IllegalArgumentException("开放平台应用凭证或回调未配置完整");
        }
        String secret = connectAppService.requirePlainSecret(app);
        String baseUrl = sysConfigService.getBaseUrl();
        connectAppService.tryFillDefaultUris(app, StringUtils.isBlank(app.getPlatformBaseUrl()) ? baseUrl : null);
        ScanLoginCredentialContext ctx = new ScanLoginCredentialContext();
        ctx.setType(AuthLoginProviderType.OAUTH2_OPEN);
        ctx.setId(appId);
        ctx.setName(StringUtils.defaultIfBlank(app.getName(), appId));
        ctx.setClientId(app.getAppId());
        ctx.setClientSecret(secret);
        ctx.setRedirectUri(app.getRedirectUri());
        ctx.setScope(StringUtils.defaultIfBlank(app.getScope(), "basic"));
        ctx.setPlatformBaseUrl(app.getPlatformBaseUrl());
        boolean sameSite = WebPathUtils.isSameSiteUrl(app.getPlatformBaseUrl(), baseUrl);
        ctx.setQrcMode(sameSite ? "as" : "rp");
        if (!sameSite && StringUtils.isNotBlank(app.getPlatformBaseUrl())) {
            ctx.setOriginUri(normalizeOrigin(app.getPlatformBaseUrl()));
        }
        ctx.setConnectApp(app);
        return ctx;
    }

    private static String normalizeOrigin(String origin) {
        if (StringUtils.isBlank(origin)) {
            return null;
        }
        String trimmed = origin.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String joinBasePath(String baseUrl, String path) {
        if (StringUtils.isBlank(baseUrl)) {
            return path;
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String p = path == null ? "" : path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return base + p;
    }
}
