package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.service.TokenStoreService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.qrc.dao.ClientGrantDao;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.DeliveryMode;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.model.TicketStatus;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.service.OpenCodeService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import cn.org.autumn.utils.Uuid;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClientGrantService extends ModuleService<ClientGrantDao, ClientGrantEntity> {

    @Autowired
    @Lazy
    private ClientDetailsService clientDetailsService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    @Lazy
    private TokenStoreService tokenStoreService;

    @Autowired
    @Lazy
    private QrcWebhookDeliveryService qrcWebhookDeliveryService;

    @Autowired(required = false)
    @Lazy
    private OpenPlatformService openPlatformService;

    @Autowired
    @Lazy
    private OpenCodeService openCodeService;

    public ClientGrantEntity getByClientId(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        return baseMapper.getByClientId(clientId);
    }

    public ClientGrantEntity getOrDefault(String clientId) {
        ClientGrantEntity grant = getByClientId(clientId);
        if (grant != null) {
            return grant;
        }
        ClientGrantEntity defaults = new ClientGrantEntity();
        defaults.setClientId(clientId);
        defaults.setEnabled(true);
        defaults.setDelivery(DeliveryMode.POLL_CODE);
        defaults.setConsent(false);
        return defaults;
    }

    public ClientGrantEntity saveGrant(ClientGrantEntity entity) {
        if (entity == null) {
            return null;
        }
        if (StringUtils.isBlank(entity.getUuid())) {
            entity.setUuid(Uuid.uuid());
        }
        entity.setUpdated(new Date());
        insertOrUpdate(entity);
        return entity;
    }

    public boolean requireAppConsent(String clientId) {
        ClientGrantEntity grant = getOrDefault(clientId);
        return grant != null && grant.isConsent();
    }

    public ClientDetailsEntity requireTrustedClient(String clientId) throws CodeException {
        ClientDetailsEntity client = clientDetailsService.findByClientId(clientId);
        if (client == null) {
            throw new CodeException("无效的客户端ID", 8602);
        }
        if (client.getTrusted() == null || client.getTrusted() == 0) {
            throw new CodeException("不受信任的客户端ID", 8603);
        }
        if (client.getArchived() != null && client.getArchived() == 1) {
            throw new CodeException("客户端ID已归档", 8604);
        }
        return client;
    }

    public void validateRedirectUri(ClientDetailsEntity client, String redirectUri) throws CodeException {
        if (client == null || StringUtils.isBlank(redirectUri)) {
            throw new CodeException("redirect_uri不能为空", 8605);
        }
        if (StringUtils.isBlank(client.getRedirectUri())) {
            throw new CodeException("客户端未配置redirect_uri", 8606);
        }
        if (!redirectUri.equalsIgnoreCase(client.getRedirectUri())) {
            throw new CodeException("redirect_uri不匹配", 8607);
        }
    }

    public ClientGrantEntity requireEnabledGrant(String clientId) throws CodeException {
        ClientGrantEntity grant = getOrDefault(clientId);
        if (!grant.isEnabled()) {
            throw new CodeException("客户端未启用扫码授权", 8630);
        }
        return grant;
    }

    public void validateClientSecret(String clientId, String clientSecret) throws CodeException {
        ClientDetailsEntity client = requireTrustedClient(clientId);
        if (StringUtils.isBlank(clientSecret) || !clientSecret.equals(client.getClientSecret())) {
            throw new CodeException("客户端密钥认证失败", 8608);
        }
    }

    private String issueDeliveryCode(String clientId, String redirectUri, SysUserEntity user) throws OAuthSystemException {
        if (isActiveOplApp(clientId)) {
            if (StringUtils.isBlank(redirectUri)) {
                throw new IllegalStateException("OPL 应用扫码授权 redirect_uri 不能为空");
            }
            OpenCodeEntity codeEntity = openCodeService.issue(clientId, user.getUuid(), redirectUri, null, null);
            return codeEntity.getCode();
        }
        return issueAuthCode(user);
    }

    private boolean isActiveOplApp(String clientId) {
        if (openPlatformService == null || StringUtils.isBlank(clientId)) {
            return false;
        }
        OpenAppSnapshot app = openPlatformService.getApp(clientId);
        return app != null && app.getStatus() == OplConstants.STATUS_ACTIVE;
    }

    public String issueAuthCode(SysUserEntity user) throws OAuthSystemException {
        OAuthIssuerImpl issuer = new OAuthIssuerImpl(new MD5Generator());
        String code = issuer.authorizationCode();
        clientDetailsService.putAuthCode(code, user);
        return code;
    }

    public String issueAccessToken(SysUserEntity user) throws OAuthSystemException {
        TokenStore tokenStore = new TokenStore(user);
        String accessToken = "";
        String refreshToken = "";
        if (sysConfigService.currentToken()) {
            TokenStoreEntity tokenStoreEntity = tokenStoreService.findByUser(user);
            if (tokenStoreEntity != null && StringUtils.isNotBlank(tokenStoreEntity.getAccessToken()) && StringUtils.isNotBlank(tokenStoreEntity.getRefreshToken())) {
                boolean validA = clientDetailsService.isValidAccessToken(tokenStoreEntity.getAccessToken());
                boolean validR = clientDetailsService.isValidRefreshToken(tokenStoreEntity.getRefreshToken());
                if (validA && validR) {
                    accessToken = tokenStoreEntity.getAccessToken();
                    refreshToken = tokenStoreEntity.getRefreshToken();
                }
            }
        }
        if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(refreshToken)) {
            OAuthIssuerImpl issuer = new OAuthIssuerImpl(new MD5Generator());
            accessToken = issuer.accessToken();
            refreshToken = issuer.refreshToken();
            clientDetailsService.putToken(accessToken, refreshToken, tokenStore);
        }
        return accessToken;
    }

    public String buildAuthorizeRedirect(String redirectUri, String code, String state, String callback) {
        StringBuilder sb = new StringBuilder(redirectUri);
        sb.append(redirectUri.contains("?") ? "&" : "?");
        sb.append("code=").append(urlEncode(code));
        if (StringUtils.isNotBlank(state)) {
            sb.append("&state=").append(urlEncode(state));
        }
        if (StringUtils.isNotBlank(callback)) {
            sb.append("&callback=").append(urlEncode(callback));
        }
        return sb.toString();
    }

    public String buildDeepLink(String scheme, String code, String state) {
        if (StringUtils.isBlank(scheme)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://oauth/callback?code=").append(urlEncode(code));
        if (StringUtils.isNotBlank(state)) {
            sb.append("&state=").append(urlEncode(state));
        }
        return sb.toString();
    }

    public ConfirmResult deliverOAuth(TicketSnapshot ticket, ClientGrantEntity grant, SysUserEntity user) throws Exception {
        String clientId = TicketPayloads.get(ticket, "clientId");
        String redirectUri = TicketPayloads.get(ticket, "redirectUri");
        String state = TicketPayloads.get(ticket, "state");
        String callback = TicketPayloads.get(ticket, "callback");
        ClientDetailsEntity client = requireTrustedClient(clientId);
        validateRedirectUri(client, redirectUri);
        String code = issueDeliveryCode(clientId, redirectUri, user);
        String delivery = grant == null ? DeliveryMode.POLL_CODE : grant.getDelivery();
        String payloadDelivery = TicketPayloads.get(ticket, "delivery");
        if (StringUtils.isNotBlank(payloadDelivery)) {
            delivery = payloadDelivery;
        }
        if (StringUtils.isBlank(delivery)) {
            delivery = DeliveryMode.POLL_CODE;
        }
        Map<String, String> result = new HashMap<>();
        result.put("code", code);
        result.put("state", state == null ? "" : state);
        result.put("clientId", clientId);
        ConfirmResult confirmResult = new ConfirmResult();
        confirmResult.setCompleted(true);
        confirmResult.setResult(result);
        if (DeliveryMode.DEEP_LINK.equals(delivery)) {
            String scheme = firstScheme(grant);
            String deepLink = buildDeepLink(scheme, code, state);
            confirmResult.setDeepLink(deepLink);
            result.put("deepLink", deepLink == null ? "" : deepLink);
        } else if (DeliveryMode.POLL_TOKEN.equals(delivery)) {
            String accessToken = issueAccessToken(user);
            if (StringUtils.isNotBlank(accessToken)) {
                result.put("accessToken", accessToken);
            }
        } else if (DeliveryMode.WEBHOOK.equals(delivery)) {
            Map<String, Object> webhookData = new HashMap<>();
            if (result != null) {
                webhookData.putAll(result);
            }
            webhookData.put("status", TicketStatus.COMPLETED);
            putScannerBrief(webhookData, user);
            log.info("QRC authorize webhook delivering uuid={} clientId={} webhook={}", ticket.getUuid(), clientId, TicketPayloads.get(ticket, "webhook"));
            qrcWebhookDeliveryService.deliverAuthorized(ticket, grant, webhookData);
        }
        if (StringUtils.isNotBlank(redirectUri) && !DeliveryMode.POLL_CODE.equals(delivery) && !DeliveryMode.POLL_TOKEN.equals(delivery) && !DeliveryMode.WEBHOOK.equals(delivery)) {
            confirmResult.setRedirect(buildAuthorizeRedirect(redirectUri, code, state, callback));
        }
        if (DeliveryMode.POLL_CODE.equals(delivery) || DeliveryMode.POLL_TOKEN.equals(delivery) || DeliveryMode.WEBHOOK.equals(delivery)) {
            confirmResult.setRedirect(null);
        } else if (StringUtils.isBlank(confirmResult.getRedirect()) && StringUtils.isNotBlank(redirectUri)) {
            confirmResult.setRedirect(buildAuthorizeRedirect(redirectUri, code, state, callback));
        }
        return confirmResult;
    }

    public void applyPollCodeRedirect(ConfirmResult result, TicketSnapshot ticket) {
        if (result == null || result.getResult() == null) {
            return;
        }
        String code = result.getResult().get("code");
        String redirectUri = TicketPayloads.get(ticket, "redirectUri");
        if (StringUtils.isBlank(code) || StringUtils.isBlank(redirectUri)) {
            return;
        }
        result.setRedirect(buildAuthorizeRedirect(redirectUri, code, TicketPayloads.get(ticket, "state"), TicketPayloads.get(ticket, "callback")));
        result.setCompleted(true);
        result.getResult().put("redirectUri", redirectUri);
    }

    private String firstScheme(ClientGrantEntity grant) {
        if (grant == null || StringUtils.isBlank(grant.getSchemes())) {
            return null;
        }
        String[] parts = grant.getSchemes().split(",");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                return part.trim();
            }
        }
        return null;
    }

    public static String sign(String payload, String secret) {
        return QrcWebhookDeliveryService.sign(payload, secret);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private static void putScannerBrief(Map<String, Object> data, SysUserEntity user) {
        if (data == null || user == null) {
            return;
        }
        Map<String, String> brief = new HashMap<>();
        brief.put("displayName", StringUtils.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername());
        if (StringUtils.isNotBlank(user.getIcon())) {
            brief.put("icon", user.getIcon());
        }
        data.put("scannerBrief", brief);
    }
}
