package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.service.TokenStoreService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.qrc.dao.ClientGrantDao;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.DeliveryMode;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.HttpClientUtils;
import cn.org.autumn.utils.Uuid;
import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClientGrantService extends ModuleService<ClientGrantDao, ClientGrantEntity> {

    private final Gson gson = new Gson();

    @Autowired
    @Lazy
    private ClientDetailsService clientDetailsService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    @Lazy
    private TokenStoreService tokenStoreService;

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

    public void validateClientSecret(String clientId, String clientSecret) throws CodeException {
        ClientDetailsEntity client = requireTrustedClient(clientId);
        if (StringUtils.isBlank(clientSecret) || !clientSecret.equals(client.getClientSecret())) {
            throw new CodeException("客户端密钥认证失败", 8608);
        }
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
        Map<String, String> payload = ticket.getPayload();
        String clientId = payload.get("clientId");
        String redirectUri = payload.get("redirectUri");
        String state = payload.get("state");
        String callback = payload.get("callback");
        ClientDetailsEntity client = requireTrustedClient(clientId);
        validateRedirectUri(client, redirectUri);
        String code = issueAuthCode(user);
        String delivery = grant == null ? DeliveryMode.POLL_CODE : grant.getDelivery();
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
            postWebhook(grant, ticket, result);
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

    private void postWebhook(ClientGrantEntity grant, TicketSnapshot ticket, Map<String, String> result) {
        if (grant == null || StringUtils.isBlank(grant.getWebhook())) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("event", "qrc.authorized");
        body.put("uuid", ticket.getUuid());
        body.put("timestamp", System.currentTimeMillis());
        body.put("data", result);
        String json = gson.toJson(body);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Qrc-Event", "qrc.authorized");
        headers.put("X-Qrc-Timestamp", String.valueOf(System.currentTimeMillis()));
        if (StringUtils.isNotBlank(grant.getSecret())) {
            headers.put("X-Qrc-Signature", sign(json, grant.getSecret()));
        }
        try {
            ScanLoginConfig config = sysConfigService.getConfigObjectValidate(ScanLoginConfig.CONFIG_KEY, ScanLoginConfig.class);
            int timeout = config == null ? new ScanLoginConfig().getWebhookTimeoutMs() : config.getWebhookTimeoutMs();
            HttpClientUtils.doPostJson(grant.getWebhook(), json, headers, timeout);
        } catch (Exception e) {
            log.warn("QRC webhook delivery failed ticket={}: {}", ticket.getUuid(), e.getMessage());
        }
    }

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
