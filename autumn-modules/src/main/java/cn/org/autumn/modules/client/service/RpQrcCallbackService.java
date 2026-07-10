package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.client.model.ScanLoginCredentialContext;
import cn.org.autumn.modules.client.oauth2.RpQrcSyntheticRequest;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectLoginService;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.opc.support.ConnectBindException.ConflictType;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.model.DeliveryMode;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.HttpClientUtils;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** RP 联邦扫码：建票注入 WEBHOOK，SSE 推送状态，入站 authorized 自动完成登录。 */
@Service
public class RpQrcCallbackService {

    @Autowired
    private AuthSiteRoleService authSiteRoleService;

    @Autowired
    private OauthRpStateService oauthRpStateService;

    @Autowired
    private WebOauthLoginService webOauthLoginService;

    @Autowired
    private WebOauthEndpointResolver webOauthEndpointResolver;

    @Autowired
    private ScanLoginCredentialService scanLoginCredentialService;

    @Autowired
    private RpQrcPendingStore rpQrcPendingStore;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private RpQrcSessionContextService rpQrcSessionContextService;

    @Autowired
    private RpQrcEventStreamService rpQrcEventStreamService;

    @Autowired
    private ConnectLoginService connectLoginService;

    public TicketCreateResult createTicket(HttpServletRequest request, String callback, String credentialType, String credentialId) {
        ScanLoginCredentialContext credential = resolveCredential(request, credentialType, credentialId);
        if (!"rp".equalsIgnoreCase(credential.getQrcMode())) {
            throw new IllegalStateException("当前凭证不支持 RP 联邦扫码，请使用同源扫码模式");
        }
        WebAuthenticationEntity rpClient = credential.getWebAuth();
        if (rpClient == null && credential.getConnectApp() != null) {
            rpClient = toVirtualWebAuth(credential);
        }
        if (rpClient == null) {
            throw new IllegalStateException("未配置 RP OAuth 客户端");
        }
        String inboundUrl = WebPathUtils.forBrowser(request, "/client/oauth2/qrc/web/inbound");
        OpenTicketCreateRequest body = new OpenTicketCreateRequest();
        body.setClientId(credential.getClientId());
        body.setClientSecret(credential.getClientSecret());
        body.setRedirectUri(credential.getRedirectUri());
        body.setScope(StringUtils.defaultIfBlank(credential.getScope(), "basic"));
        body.setState(oauthRpStateService.issueState(WebPathUtils.safeOauthCallbackForClient(request, callback)));
        Map<String, String> payload = new HashMap<>();
        payload.put("delivery", DeliveryMode.WEBHOOK);
        payload.put("webhook", inboundUrl);
        payload.put("webhookSecret", credential.getClientSecret());
        body.setPayload(payload);
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("data", body);
        String url = resolveOpenCreateUri(credential, rpClient);
        String raw = HttpClientUtils.doPostJson(url, JSON.toJSONString(envelope));
        Response<TicketCreateResult> response = JSON.parseObject(raw, new TypeReference<Response<TicketCreateResult>>() {
        });
        if (response == null || !response.success() || response.getData() == null) {
            throw new IllegalStateException(response == null ? "建票失败" : StringUtils.defaultIfBlank(response.getMsg(), "建票失败"));
        }
        TicketCreateResult created = response.getData();
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid(created.getUuid());
        pending.setStatus("PENDING");
        pending.setClientId(credential.getClientId());
        pending.setCredentialType(credential.getType());
        pending.setCredentialId(credential.getId());
        pending.setBrowserSessionId(request.getSession().getId());
        pending.setCallback(WebPathUtils.safeOauthCallbackForClient(request, callback));
        ScanLoginConfig config = sysConfigService.getConfigObject(ScanLoginConfig.CONFIG_KEY, ScanLoginConfig.class);
        int ttlSeconds = config == null ? 300 : config.getTicketTtlSeconds();
        pending.setExpiredAt(System.currentTimeMillis() + ttlSeconds * 1000L);
        rpQrcPendingStore.save(pending);
        return created;
    }

    public void completeOnInbound(RpQrcPendingSession pending, String code) {
        if (pending == null || StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("回调未包含授权码");
        }
        if (StringUtils.isBlank(pending.getBrowserSessionId())) {
            markSessionExpired(pending);
            return;
        }
        ScanLoginCredentialContext credential = scanLoginCredentialService.require(pending.getCredentialType(), pending.getCredentialId());
        final ScanLoginCredentialContext cred = credential;
        final String authCode = code;
        boolean executed = rpQrcSessionContextService.runWithBrowserSession(pending.getBrowserSessionId(), () -> finishInboundLogin(pending, cred, authCode));
        if (!executed) {
            markSessionExpired(pending);
            return;
        }
        rpQrcPendingStore.save(pending);
        rpQrcEventStreamService.publish(pending);
        rpQrcPendingStore.remove(pending.getUuid());
    }

    public void applyScanned(RpQrcPendingSession pending, JSONObject data) {
        if (pending == null) {
            return;
        }
        pending.setStatus("SCANNED");
        if (data != null) {
            JSONObject briefJson = data.getJSONObject("scannerBrief");
            if (briefJson != null) {
                pending.setScannerBrief(briefJson.toJavaObject(ScannerBrief.class));
            }
        }
        rpQrcPendingStore.save(pending);
        rpQrcEventStreamService.publish(pending);
    }

    private void finishInboundLogin(RpQrcPendingSession pending, ScanLoginCredentialContext credential, String code) {
        RpQrcSyntheticRequest request = new RpQrcSyntheticRequest(pending.getCallback(), sysConfigService.getBaseUrl());
        if (AuthLoginProviderType.OAUTH2_OPEN.equalsIgnoreCase(credential.getType())) {
            finishOpenInboundLogin(pending, request, credential, code);
            return;
        }
        WebAuthenticationEntity rpClient = credential.getWebAuth();
        if (rpClient == null) {
            throw new IllegalStateException("未配置 RP OAuth 客户端");
        }
        try {
            webOauthLoginService.completeRemoteOAuthCallback(request, rpClient, code);
        } catch (WebOauthBindException e) {
            if (e.getConflictType() == WebOauthBindException.ConflictType.BIND_CHOICE_REQUIRED && StringUtils.isNotBlank(e.getPendingToken())) {
                pending.setRedirectUrl(webOauthLoginService.bindChoicePageUrl(request, e.getPendingToken()));
                pending.setStatus("COMPLETED");
                return;
            }
            throw e;
        }
        applyCompletedRedirect(pending, request, code);
    }

    private void finishOpenInboundLogin(RpQrcPendingSession pending, RpQrcSyntheticRequest request, ScanLoginCredentialContext credential, String code) {
        ConnectAppEntity app = credential.getConnectApp();
        if (app == null) {
            throw new IllegalStateException("未配置开放平台应用");
        }
        try {
            connectLoginService.completeOAuthCallback(request, app, code, pending.getCallback());
        } catch (ConnectBindException e) {
            if (e.getConflictType() == ConflictType.BIND_CHOICE_REQUIRED && StringUtils.isNotBlank(e.getPendingToken())) {
                pending.setRedirectUrl(connectLoginService.bindChoicePageUrl(request, app.getAppId(), e.getPendingToken()));
                pending.setStatus("COMPLETED");
                return;
            }
            throw e;
        }
        applyCompletedRedirect(pending, request, code);
    }

    private void applyCompletedRedirect(RpQrcPendingSession pending, RpQrcSyntheticRequest request, String code) {
        String redirect = pending.getCallback();
        if (StringUtils.isBlank(redirect)) {
            redirect = WebPathUtils.forBrowser(request, "/");
        }
        pending.setRedirectUrl(redirect);
        pending.setStatus("COMPLETED");
        pending.setCode(code);
    }

    private void markSessionExpired(RpQrcPendingSession pending) {
        pending.setStatus("SESSION_EXPIRED");
        rpQrcPendingStore.save(pending);
        rpQrcEventStreamService.publish(pending);
    }

    private ScanLoginCredentialContext resolveCredential(HttpServletRequest request, String credentialType, String credentialId) {
        if (StringUtils.isNotBlank(credentialType) && StringUtils.isNotBlank(credentialId)) {
            return scanLoginCredentialService.require(credentialType, credentialId);
        }
        WebAuthenticationEntity rpClient = requireRpClient(request);
        return scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_CLASSIC, rpClient.getClientId());
    }

    private WebAuthenticationEntity requireRpClient(HttpServletRequest request) {
        if (!authSiteRoleService.isRpEnabled()) {
            throw new IllegalStateException("当前站点未启用 RP 角色");
        }
        WebAuthenticationEntity rpClient = authSiteRoleService.resolveRpClient(request);
        if (rpClient == null) {
            throw new IllegalStateException("未配置 RP OAuth 客户端");
        }
        return rpClient;
    }

    private String resolveOpenCreateUri(ScanLoginCredentialContext credential, WebAuthenticationEntity rpClient) {
        if (credential.getWebAuth() != null) {
            return webOauthEndpointResolver.resolveQrcOpenCreateUri(rpClient);
        }
        if (StringUtils.isNotBlank(credential.getOriginUri())) {
            return credential.getOriginUri() + WebOauthEndpointResolver.PATH_QRC_OPEN_CREATE;
        }
        throw new IllegalStateException("未配置远程平台地址");
    }

    private WebAuthenticationEntity toVirtualWebAuth(ScanLoginCredentialContext credential) {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        web.setClientId(credential.getClientId());
        web.setClientSecret(credential.getClientSecret());
        web.setRedirectUri(credential.getRedirectUri());
        web.setScope(credential.getScope());
        web.setOriginUri(credential.getOriginUri());
        web.setName(credential.getName());
        return web;
    }
}
