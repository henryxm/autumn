package cn.org.autumn.modules.opl.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.oauth.oauth2.support.RedirectUriSupport;
import cn.org.autumn.modules.opl.dao.OpenAppDao;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenPlatformEvent;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.opl.support.OplSnapshots;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAppService extends ModuleService<OpenAppDao, OpenAppEntity> {

    private static final String DEFAULT_APP_SECRET_PEPPER = "opl_app_secret_v1";
    public static final String CONFIG_APP_SECRET_PEPPER = "OPL_APP_SECRET_PEPPER";

    @Autowired
    private OplExtensionService oplExtensionService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AuthScopeSupport authScopeSupport;

    public OpenAppEntity getByAppId(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        return baseMapper.getByAppId(appId);
    }

    public List<OpenAppEntity> listByAccount(String accountUuid) {
        if (StringUtils.isBlank(accountUuid)) {
            return new ArrayList<>();
        }
        List<OpenAppEntity> list = baseMapper.listByAccount(accountUuid);
        return list == null ? new ArrayList<>() : list;
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppRegisterOutcome register(String accountUuid, String name, String redirectUri, String scope, OpenAppType appType) {
        if (StringUtils.isBlank(accountUuid)) {
            throw new IllegalArgumentException("开发者账号不能为空");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("应用名称不能为空");
        }
        if (StringUtils.isBlank(redirectUri)) {
            throw new IllegalArgumentException("回调地址不能为空");
        }
        boolean allowHttp = sysConfigService != null && sysConfigService.getBoolean(RedirectUriSupport.CONFIG_ALLOW_HTTP);
        RedirectUriSupport.validateFormat(redirectUri, allowHttp);
        OpenAppType resolvedType = appType == null ? OpenAppType.Web : appType;
        try {
            oplExtensionService.validateRegister(accountUuid, name, redirectUri, scope, resolvedType);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        String appId = generateAppId();
        String plainSecret = generateAppSecret();
        String resolvedScope = StringUtils.defaultIfBlank(scope, OplConstants.DEFAULT_SCOPE);
        validateOplScope(resolvedScope);
        OpenAppEntity app = new OpenAppEntity();
        app.setAccount(accountUuid);
        app.setAppId(appId);
        app.setName(name.trim());
        app.setAppType(resolvedType);
        app.setRedirectUri(redirectUri.trim());
        app.setScope(resolvedScope);
        app.setStatus(OpenAppEntity.STATUS_ACTIVE);
        applySecretHash(app, plainSecret);
        Date now = new Date();
        app.setCreate(now);
        app.setUpdate(now);
        insert(app);
        OpenPlatformEvent event = OpenPlatformEvent.of(OplConstants.Event.APP_REGISTERED);
        event.setAppId(appId);
        event.setAccount(accountUuid);
        event.getPayload().put("appType", resolvedType.name());
        oplExtensionService.publish(event);
        return OpenAppRegisterOutcome.of(appId, plainSecret, app.getName(), app.getRedirectUri(), app.getScope(), app.getAppType());
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppRegisterOutcome resetSecret(String accountUuid, String appId) {
        OpenAppEntity app = requireOwnedApp(accountUuid, appId);
        String plainSecret = generateAppSecret();
        applySecretHash(app, plainSecret);
        app.setUpdate(new Date());
        updateById(app);
        OpenPlatformEvent event = OpenPlatformEvent.of(OplConstants.Event.APP_SECRET_RESET);
        event.setAppId(app.getAppId());
        event.setAccount(app.getAccount());
        oplExtensionService.publish(event);
        return OpenAppRegisterOutcome.of(app.getAppId(), plainSecret, app.getName(), app.getRedirectUri(), app.getScope(), app.getAppType());
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppEntity updateApp(String accountUuid, String appId, String name, String redirectUri, String scope, OpenAppType appType) {
        OpenAppEntity app = requireOwnedApp(accountUuid, appId);
        if (StringUtils.isNotBlank(name)) {
            app.setName(name.trim());
        }
        if (appType != null) {
            app.setAppType(appType);
        }
        if (StringUtils.isNotBlank(redirectUri)) {
            boolean allowHttp = sysConfigService != null && sysConfigService.getBoolean(RedirectUriSupport.CONFIG_ALLOW_HTTP);
            RedirectUriSupport.validateFormat(redirectUri, allowHttp);
            app.setRedirectUri(redirectUri.trim());
        }
        if (StringUtils.isNotBlank(scope)) {
            validateOplScope(scope.trim());
            app.setScope(scope.trim());
        }
        app.setUpdate(new Date());
        updateById(app);
        return app;
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppEntity updateStatus(String appId, int status) {
        OpenAppEntity app = getByAppId(appId);
        if (app == null) {
            throw new IllegalArgumentException("应用不存在");
        }
        if (status != OpenAppEntity.STATUS_ACTIVE && status != OpenAppEntity.STATUS_DISABLED) {
            throw new IllegalArgumentException("无效的状态值");
        }
        app.setStatus(status);
        app.setUpdate(new Date());
        updateById(app);
        return app;
    }

    public boolean validateSecret(String appId, String plainSecret) {
        OpenAppEntity app = getByAppId(appId);
        if (app == null || app.getStatus() != OpenAppEntity.STATUS_ACTIVE) {
            return false;
        }
        if (StringUtils.isBlank(plainSecret)) {
            return false;
        }
        return hashSecret(plainSecret, app.getAppSecretSalt()).equals(app.getAppSecretHash());
    }

    public OpenAppEntity requireActiveApp(String appId) {
        OpenAppEntity app = getByAppId(appId);
        if (app == null) {
            throw new IllegalArgumentException("无效的appId");
        }
        if (app.getStatus() != OpenAppEntity.STATUS_ACTIVE) {
            throw new IllegalStateException("应用已禁用");
        }
        return app;
    }

    public void validateRedirectUri(OpenAppEntity app, String redirectUri) {
        if (app == null || StringUtils.isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri不能为空");
        }
        if (StringUtils.isBlank(app.getRedirectUri())) {
            throw new IllegalArgumentException("应用未配置回调地址");
        }
        try {
            oplExtensionService.validateRedirectUri(OplSnapshots.toAppSnapshot(app), redirectUri);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (!oplExtensionService.isRelaxedRedirectMatch(OplSnapshots.toAppSnapshot(app)) && !StringUtils.equals(redirectUri, app.getRedirectUri())) {
            throw new IllegalArgumentException("redirect_uri不匹配");
        }
        if (oplExtensionService.isRelaxedRedirectMatch(OplSnapshots.toAppSnapshot(app)) && !redirectUri.equalsIgnoreCase(app.getRedirectUri())) {
            throw new IllegalArgumentException("redirect_uri不匹配");
        }
    }

    private OpenAppEntity requireOwnedApp(String accountUuid, String appId) {
        OpenAppEntity app = getByAppId(appId);
        if (app == null || !StringUtils.equals(app.getAccount(), accountUuid)) {
            throw new IllegalArgumentException("应用不存在或无权限");
        }
        return app;
    }

    private void applySecretHash(OpenAppEntity app, String plainSecret) {
        String salt = Md5.md5((app.getAppId() + System.nanoTime() + Uuid.uuid()).getBytes()).substring(0, 16);
        app.setAppSecretSalt(salt);
        app.setAppSecretHash(hashSecret(plainSecret, salt));
    }

    private String hashSecret(String plainSecret, String salt) {
        return ShiroUtils.sha256(plainSecret + resolveAppSecretPepper(), salt);
    }

    private String resolveAppSecretPepper() {
        if (sysConfigService == null) {
            return DEFAULT_APP_SECRET_PEPPER;
        }
        String configured = sysConfigService.getValue(CONFIG_APP_SECRET_PEPPER);
        return StringUtils.isBlank(configured) ? DEFAULT_APP_SECRET_PEPPER : configured.trim();
    }

    private String generateAppId() {
        for (int i = 0; i < 10; i++) {
            String appId = "ax" + Uuid.uuid().substring(0, 16);
            if (getByAppId(appId) == null) {
                return appId;
            }
        }
        throw new IllegalStateException("appId生成失败");
    }

    private String generateAppSecret() {
        return Uuid.uuid() + Uuid.uuid().substring(0, 8);
    }

    private void validateOplScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            return;
        }
        OpenAppEntity helper = new OpenAppEntity();
        helper.setScope(scope.trim());
        try {
            authScopeSupport.validateOplScope(OplSnapshots.toAppSnapshot(helper), scope.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的授权范围: " + scope.trim(), e);
        }
    }
}
