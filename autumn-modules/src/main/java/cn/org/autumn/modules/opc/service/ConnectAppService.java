package cn.org.autumn.modules.opc.service;

import cn.org.autumn.base.EncryptModuleService;
import cn.org.autumn.modules.oauth.oauth2.support.RedirectUriSupport;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.modules.opc.dao.ConnectAppDao;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.OpcSnapshots;
import cn.org.autumn.opc.model.ConnectAppSnapshot;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.HttpClientUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectAppService extends EncryptModuleService<ConnectAppDao, ConnectAppEntity> {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

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

    public void stripSecret(ConnectAppEntity app) {
        if (app != null) {
            app.setAppSecret(null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity saveConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                       String redirectUri, String name, String scope) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(appSecret)) {
            throw new IllegalArgumentException("appId与appSecret不能为空");
        }
        return persistConfig(userUuid, appId, appSecret, platformBaseUrl, redirectUri, name, scope, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                         String redirectUri, String name, String scope) {
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
        return persistConfig(owner, appId, appSecret, platformBaseUrl, redirectUri, name, scope, true);
    }

    private ConnectAppEntity persistConfig(String userUuid, String appId, String appSecret, String platformBaseUrl,
                                           String redirectUri, String name, String scope, boolean updating) {
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
        app.setScope(StringUtils.defaultIfBlank(scope, OplConstants.DEFAULT_SCOPE));
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
        stripSecret(app);
        return app;
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

    public boolean isAutoRegisterEnabled() {
        if (sysConfigService == null) {
            return false;
        }
        String value = sysConfigService.getValue(OpcConstants.CONFIG_AUTO_REGISTER);
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return sysConfigService.getBoolean(OpcConstants.CONFIG_AUTO_REGISTER);
    }

    public void setAutoRegisterEnabled(boolean enabled) {
        if (sysConfigService == null) {
            return;
        }
        String value = enabled ? "true" : "false";
        if (sysConfigService.hasKey(OpcConstants.CONFIG_AUTO_REGISTER)) {
            sysConfigService.updateValueByKey(OpcConstants.CONFIG_AUTO_REGISTER, value);
            return;
        }
        SysConfigEntity config = new SysConfigEntity();
        config.setParamKey(OpcConstants.CONFIG_AUTO_REGISTER);
        config.setParamValue(value);
        config.setStatus(1);
        config.setRemark("开放接入：OAuth回调无绑定时是否自动注册本地用户");
        sysConfigService.save(config);
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
}
