package cn.org.autumn.modules.opc.service;

import cn.org.autumn.base.EncryptModuleService;
import cn.org.autumn.config.UsingHandler;
import cn.org.autumn.model.PageLoginSupport;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.oauth.oauth2.support.RedirectUriSupport;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.modules.opc.dao.ConnectAppDao;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.OpcSnapshots;
import cn.org.autumn.opc.model.ConnectAppSnapshot;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.HttpClientUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectAppService extends EncryptModuleService<ConnectAppDao, ConnectAppEntity> implements UsingHandler {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    @Lazy
    private ClientDetailsService clientDetailsService;

    @Autowired
    private AuthScopeSupport authScopeSupport;

    @Override
    public boolean using(Object value) {
        return isIconHashInUse(value);
    }

    /**
     * 登录页图标文件 hash 是否仍被 OPC 接入应用引用。
     */
    public boolean isIconHashInUse(Object hash) {
        if (hash == null) {
            return false;
        }
        String normalized = normalizeIconHash(String.valueOf(hash));
        if (StringUtils.isBlank(normalized)) {
            return false;
        }
        return countByIconHash(normalized) > 0;
    }

    public int countByIconHash(String hash) {
        String normalized = normalizeIconHash(hash);
        if (StringUtils.isBlank(normalized)) {
            return 0;
        }
        return baseMapper.countByHashInUse(normalized);
    }

    public static String normalizeIconHash(String hash) {
        if (StringUtils.isBlank(hash)) {
            return null;
        }
        return hash.trim();
    }

    /**
     * 更新 OPC 接入应用的登录页图标与文件 hash（扩展项目可直接调用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateIcon(String appId, String icon, String hash) {
        if (StringUtils.isBlank(appId)) {
            throw new IllegalArgumentException("appId不能为空");
        }
        ConnectAppEntity app = getByAppId(appId.trim());
        if (app == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        applyIcon(app, icon, hash);
        app.setUpdate(new Date());
        updateById(app);
        stripSecret(app);
        return app;
    }

    public void applyIcon(ConnectAppEntity app, String icon, String hash) {
        if (app == null) {
            return;
        }
        if (icon != null) {
            app.setIcon(StringUtils.trimToEmpty(icon));
        }
        if (hash != null) {
            app.setHash(normalizeIconHash(hash));
        } else if (icon != null && StringUtils.isBlank(app.getIcon())) {
            app.setHash(null);
        }
    }

    public ConnectAppEntity getByAppId(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        return afterRead(baseMapper.getByAppId(appId));
    }

    public List<ConnectAppEntity> listByUser(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        return afterRead(baseMapper.listByUser(userUuid));
    }

    public List<ConnectAppSnapshot> listSnapshotsByUser(String userUuid) {
        return OpcSnapshots.toSnapshots(listByUser(userUuid));
    }

    public ConnectAppSnapshot toPublicSnapshot(ConnectAppEntity app) {
        stripSecret(app);
        return OpcSnapshots.toSnapshot(app);
    }

    public String requirePlainSecret(String appId) {
        ConnectAppEntity app = getByAppId(appId);
        return requirePlainSecret(app);
    }

    public String requirePlainSecret(ConnectAppEntity app) {
        if (app == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        afterRead(app);
        if (StringUtils.isBlank(app.getAppSecret())) {
            throw new IllegalStateException("appSecret未配置");
        }
        return app.getAppSecret();
    }

    /** 登录页 Provider 准入：appSecret 已落库且可读（不抛异常）。 */
    public boolean hasConfiguredSecret(ConnectAppEntity app) {
        if (app == null || StringUtils.isBlank(app.getAppId())) {
            return false;
        }
        try {
            if (StringUtils.isNotBlank(app.getAppSecret())) {
                return true;
            }
            afterRead(app);
            if (StringUtils.isNotBlank(app.getAppSecret())) {
                return true;
            }
        } catch (Exception ignored) {
            // fall through to raw column check
        }
        return baseMapper.countSecretByAppId(app.getAppId().trim()) > 0;
    }

    /** 登录页显式展示的活跃 OPC 接入应用（显式 SQL，避免 Wrapper 列名差异）。 */
    public List<ConnectAppEntity> listPageLoginActive() {
        List<ConnectAppEntity> rows = baseMapper.listPageLoginActive();
        return afterRead(rows);
    }

    /** 登录页扫码展示的活跃 OPC 接入应用。 */
    public List<ConnectAppEntity> listPageQrActive() {
        List<ConnectAppEntity> rows = baseMapper.listPageQrActive();
        return afterRead(rows);
    }

    /** 推断 OPC 授权根地址；同实例本地 OPL 可回退 {@link SysConfigService#getBaseUrl()}。 */
    public String resolvePlatformBaseUrl(ConnectAppEntity app, String fallbackBaseUrl) {
        if (app == null) {
            return null;
        }
        if (StringUtils.isNotBlank(app.getPlatformBaseUrl())) {
            return normalizeBaseUrlQuiet(app.getPlatformBaseUrl());
        }
        if (StringUtils.isNotBlank(fallbackBaseUrl)) {
            return normalizeBaseUrlQuiet(fallbackBaseUrl);
        }
        return null;
    }

    /** 填充 OPC OAuth URI；platformBaseUrl 为空时返回 false，不抛异常。 */
    public boolean tryFillDefaultUris(ConnectAppEntity app, String fallbackBaseUrl) {
        if (app == null) {
            return false;
        }
        String base = resolvePlatformBaseUrl(app, fallbackBaseUrl);
        if (StringUtils.isBlank(base)) {
            return StringUtils.isNotBlank(app.getAuthorizeUri());
        }
        try {
            app.setPlatformBaseUrl(base);
            fillDefaultUris(app);
            return StringUtils.isNotBlank(app.getAuthorizeUri());
        } catch (Exception e) {
            return StringUtils.isNotBlank(app.getAuthorizeUri());
        }
    }

    private String normalizeBaseUrlQuiet(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            return null;
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    public void stripSecret(ConnectAppEntity app) {
        if (app != null) {
            app.setAppSecret(null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity saveConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                       String redirectUri, String name, String scope) {
        return saveConfig(userUuid, appId, appSecret, platformBaseUrl, redirectUri, name, scope, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity saveConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                       String redirectUri, String name, String scope, String icon, String hash, Integer pageLogin) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(appSecret)) {
            throw new IllegalArgumentException("appId与appSecret不能为空");
        }
        return persistConfig(userUuid, appId, appSecret, platformBaseUrl, redirectUri, name, scope, icon, hash, pageLogin, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                         String redirectUri, String name, String scope) {
        return updateConfig(userUuid, appId, appSecret, platformBaseUrl, redirectUri, name, scope, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                         String redirectUri, String name, String scope, String icon, String hash, Integer pageLogin) {
        if (StringUtils.isBlank(appId)) {
            throw new IllegalArgumentException("appId不能为空");
        }
        ConnectAppEntity existing = getByAppId(appId);
        if (existing == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        if (StringUtils.isBlank(appSecret)) {
            afterRead(existing);
            appSecret = existing.getAppSecret();
        }
        if (StringUtils.isBlank(appSecret)) {
            throw new IllegalArgumentException("appSecret不能为空");
        }
        String owner = StringUtils.isBlank(userUuid) ? existing.getUser() : userUuid;
        return persistConfig(owner, appId, appSecret, platformBaseUrl, redirectUri, name, scope, icon, hash, pageLogin, true);
    }

    private ConnectAppEntity persistConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                           String redirectUri, String name, String scope, String icon, String hash, Integer pageLogin, boolean updating) {
        if (StringUtils.isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirectUri不能为空");
        }
        if (StringUtils.isBlank(platformBaseUrl)) {
            throw new IllegalArgumentException("platformBaseUrl不能为空");
        }
        boolean allowHttp = sysConfigService != null && sysConfigService.getBoolean(RedirectUriSupport.CONFIG_ALLOW_HTTP);
        RedirectUriSupport.validateFormat(redirectUri, allowHttp);
        ConnectAppEntity existing = getByAppId(appId);
        if (existing != null && !StringUtils.equals(existing.getUser(), userUuid)) {
            throw new IllegalArgumentException("appId已被其他用户占用");
        }
        if (existing == null && updating) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        ConnectAppEntity app = existing == null ? new ConnectAppEntity() : existing;
        app.setUser(userUuid);
        app.setAppId(appId.trim());
        app.setAppSecret(appSecret.trim());
        app.setPlatformBaseUrl(normalizeBaseUrl(platformBaseUrl));
        app.setRedirectUri(redirectUri.trim());
        app.setName(StringUtils.defaultIfBlank(name, appId));
        String resolvedScope = StringUtils.defaultIfBlank(scope, OplConstants.DEFAULT_SCOPE);
        validateOplScope(resolvedScope);
        app.setScope(resolvedScope);
        applyIcon(app, icon, hash);
        if (pageLogin != null) {
            app.setPageLogin(pageLogin);
        }
        fillDefaultUris(app);
        app.setStatus(ConnectAppEntity.STATUS_ACTIVE);
        Date now = new Date();
        if (existing == null) {
            app.setCreate(now);
            app.setUpdate(now);
            insert(app);
        } else {
            app.setUpdate(now);
            updateById(app);
        }
        syncQrcOAuthClient(app);
        stripSecret(app);
        return app;
    }

    /** pageLogin 含 QR 时，在 AS 侧同步 oauth_client_details（appId = clientId）。 */
    private void syncQrcOAuthClient(ConnectAppEntity app) {
        if (app == null || !PageLoginSupport.showQr(app.getPageLogin())) {
            return;
        }
        afterRead(app);
        if (StringUtils.isBlank(app.getAppSecret())) {
            return;
        }
        clientDetailsService.ensureQrcOpenClient(app.getPlatformBaseUrl(), app.getAppId(), app.getAppSecret(), app.getRedirectUri(), app.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity applyToPlatform(String userUuid, String platformBaseUrl, String name, String redirectUri, String scope, String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            throw new IllegalArgumentException("申请appId需要平台访问令牌");
        }
        String base = normalizeBaseUrl(platformBaseUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Content-Type", "application/json");
        JSONObject body = new JSONObject();
        body.put("data", buildRegisterPayload(name, redirectUri, scope));
        String response = HttpClientUtils.doPostJson(base + OplConstants.API_PLATFORM + "/app/register", body.toJSONString(), headers, 10000);
        if (StringUtils.isBlank(response)) {
            throw new IllegalStateException("向开放平台申请appId失败");
        }
        JSONObject json = JSON.parseObject(response);
        if (json == null || json.getIntValue("code") != 0) {
            throw new IllegalStateException(json == null ? "申请失败" : json.getString("msg"));
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            throw new IllegalStateException("申请响应无效");
        }
        return saveConfig(userUuid, data.getString("appId"), data.getString("appSecret"), base, redirectUri, name, scope);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateStatus(String appId, int status) {
        ConnectAppEntity app = getByAppId(appId);
        if (app == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        if (status != ConnectAppEntity.STATUS_ACTIVE && status != ConnectAppEntity.STATUS_DISABLED) {
            throw new IllegalArgumentException("无效的状态值");
        }
        app.setStatus(status);
        app.setUpdate(new Date());
        updateById(app);
        stripSecret(app);
        return app;
    }

    /** 在线申请落库后补写 {@code pageLogin}（申请接口本身不返回该字段）。 */
    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity applyPageLogin(ConnectAppEntity app, Integer pageLogin) {
        if (app == null || pageLogin == null) {
            return app;
        }
        app.setPageLogin(pageLogin);
        app.setUpdate(new Date());
        updateById(app);
        stripSecret(app);
        return app;
    }

    public void fillDefaultUris(ConnectAppEntity app) {
        String base = normalizeBaseUrl(app.getPlatformBaseUrl());
        if (StringUtils.isBlank(app.getAuthorizeUri())) {
            app.setAuthorizeUri(base + OplConstants.OAUTH2_BASE + "/authorize");
        }
        if (StringUtils.isBlank(app.getTokenUri())) {
            app.setTokenUri(base + OplConstants.OAUTH2_BASE + "/token");
        }
        if (StringUtils.isBlank(app.getUserInfoUri())) {
            app.setUserInfoUri(base + OplConstants.OAUTH2_BASE + "/userInfo");
        }
    }

    public String buildAuthorizeEntryUrl(String appId) {
        return OpcConstants.OAUTH2_AUTHORIZE + "?appId=" + appId;
    }

    private JSONObject buildRegisterPayload(String name, String redirectUri, String scope) {
        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("redirectUri", redirectUri);
        payload.put("scope", scope);
        return payload;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("platformBaseUrl不能为空");
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private void validateOplScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            return;
        }
        OpenAppSnapshot snapshot = new OpenAppSnapshot();
        snapshot.setScope(scope.trim());
        try {
            authScopeSupport.validateOplScope(snapshot, scope.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的授权范围: " + scope.trim(), e);
        }
    }
}
