package cn.org.autumn.modules.opl.service;

import cn.org.autumn.modules.oauth.oauth2.support.PkceSupport;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.store.OplTokenContext;
import cn.org.autumn.modules.opl.support.OplSnapshots;
import cn.org.autumn.opl.model.OpenAccountSnapshot;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@link OpenPlatformService} 默认实现：委托领域 Service，对外统一 Snapshot 视图。
 */
@Service
public class OpenPlatformServiceImpl implements OpenPlatformService {

    @Autowired
    private OpenAccountService openAccountService;

    @Autowired
    private OpenAppService openAppService;

    @Autowired
    private OpenIdentityService openIdentityService;

    @Autowired
    private OpenCodeService openCodeService;

    @Autowired
    private OpenTokenService openTokenService;

    @Autowired
    private OplExtensionService oplExtensionService;

    @Autowired
    private UserProfileService userProfileService;

    @Override
    public OpenAccountSnapshot getOrCreateAccount(String userUuid, String name) {
        return OplSnapshots.toAccountSnapshot(openAccountService.getOrCreateByUser(userUuid, name));
    }

    @Override
    public OpenAccountSnapshot getAccountByUser(String userUuid) {
        return OplSnapshots.toAccountSnapshot(openAccountService.getByUser(userUuid));
    }

    @Override
    public OpenAccountSnapshot requireActiveAccount(String accountUuid) {
        return OplSnapshots.toAccountSnapshot(openAccountService.requireActiveAccount(accountUuid));
    }

    @Override
    public OpenAppSnapshot getApp(String appId) {
        return OplSnapshots.toAppSnapshot(openAppService.getByAppId(appId));
    }

    @Override
    public OpenAppSnapshot requireActiveApp(String appId) {
        return OplSnapshots.toAppSnapshot(openAppService.requireActiveApp(appId));
    }

    @Override
    public List<OpenAppSnapshot> listAppsByAccount(String accountUuid) {
        return OplSnapshots.toAppSnapshots(openAppService.listByAccount(accountUuid));
    }

    @Override
    public OpenAppRegisterOutcome registerApp(String accountUuid, String name, String redirectUri, String scope, OpenAppType appType) {
        return openAppService.register(accountUuid, name, redirectUri, scope, appType);
    }

    @Override
    public OpenAppSnapshot updateApp(String accountUuid, String appId, String name, String redirectUri, String scope, OpenAppType appType) {
        return OplSnapshots.toAppSnapshot(openAppService.updateApp(accountUuid, appId, name, redirectUri, scope, appType));
    }

    @Override
    public OpenAppSnapshot updateAppStatus(String appId, int status) {
        return OplSnapshots.toAppSnapshot(openAppService.updateStatus(appId, status));
    }

    @Override
    public OpenAppRegisterOutcome resetAppSecret(String accountUuid, String appId) {
        return openAppService.resetSecret(accountUuid, appId);
    }

    @Override
    public boolean validateAppSecret(String appId, String plainSecret) {
        return openAppService.validateSecret(appId, plainSecret);
    }

    @Override
    public void validateRedirectUri(String appId, String redirectUri) {
        OpenAppEntity app = openAppService.requireActiveApp(appId);
        openAppService.validateRedirectUri(app, redirectUri);
    }

    @Override
    public OpenIdentitySnapshot resolveIdentity(String appId, String userUuid) {
        return openIdentityService.resolveOrCreate(appId, userUuid);
    }

    @Override
    public OpenIdentitySnapshot getIdentityByOpenId(String openId) {
        return openIdentityService.getIdentityByOpenId(openId);
    }

    @Override
    public OpenIdentitySnapshot getIdentity(String appId, String userUuid) {
        return openIdentityService.getIdentity(appId, userUuid);
    }

    @Override
    public OpenUserInfoSnapshot buildUserInfo(String accessToken) {
        if (StringUtils.isBlank(accessToken) || !openTokenService.isValidAccessToken(accessToken)) {
            return null;
        }
        OplTokenContext context = openTokenService.getByAccessToken(accessToken);
        if (context == null) {
            return null;
        }
        OpenAppEntity app = openAppService.getByAppId(context.getAppId());
        OpenAppSnapshot appSnapshot = OplSnapshots.toAppSnapshot(app);
        OpenIdentitySnapshot identity = new OpenIdentitySnapshot();
        identity.setOpenId(context.getOpenId());
        identity.setUnionId(context.getUnionId());
        identity.setAppId(context.getAppId());
        identity.setUser(context.getUser());
        if (app != null) {
            identity.setAccount(app.getAccount());
        }
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        snapshot.setOpenId(context.getOpenId());
        snapshot.setUnionId(context.getUnionId());
        UserProfileEntity profile = userProfileService.getByUuid(context.getUser());
        if (profile != null) {
            snapshot.setNickname(profile.getNickname());
            snapshot.setIcon(profile.getIcon());
        }
        oplExtensionService.enrichUserInfo(appSnapshot, identity, snapshot);
        return snapshot;
    }

    @Override
    public OpenTokenSnapshot issueTokenFromCode(String appId, String code, String redirectUri) {
        return issueTokenFromCode(appId, code, redirectUri, null);
    }

    @Override
    public OpenTokenSnapshot issueTokenFromCode(String appId, String code, String redirectUri, String codeVerifier) {
        if (StringUtils.isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri不能为空");
        }
        OpenCodeEntity codeEntity = openCodeService.consume(code);
        if (codeEntity == null || !StringUtils.equals(codeEntity.getAppId(), appId)) {
            return null;
        }
        if (!StringUtils.equals(redirectUri, codeEntity.getRedirectUri())) {
            throw new IllegalArgumentException("redirect_uri不匹配");
        }
        PkceSupport.validateVerifier(codeEntity.getCodeChallenge(), codeEntity.getCodeChallengeMethod(), codeVerifier);
        OplTokenContext context = openTokenService.issueFromCode(codeEntity);
        OpenAppSnapshot app = OplSnapshots.toAppSnapshot(openAppService.getByAppId(appId));
        OpenTokenSnapshot snapshot = OplSnapshots.toTokenSnapshot(context);
        oplExtensionService.afterTokenIssued(app, snapshot);
        return snapshot;
    }

    @Override
    public OpenTokenSnapshot refreshToken(String appId, String refreshToken) {
        if (!openTokenService.isValidRefreshToken(refreshToken)) {
            return null;
        }
        OplTokenContext context = openTokenService.refresh(refreshToken);
        if (context == null || !StringUtils.equals(context.getAppId(), appId)) {
            return null;
        }
        return OplSnapshots.toTokenSnapshot(context);
    }
}
