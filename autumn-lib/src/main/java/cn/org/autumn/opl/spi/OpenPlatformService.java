package cn.org.autumn.opl.spi;

import cn.org.autumn.opl.model.OpenAccountSnapshot;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import java.util.List;

/**
 * OPL 对外服务：业务模块注入本接口完成 Account/App/Identity/OAuth 编程式调用。
 * <p>
 * 默认实现 {@code OpenPlatformServiceImpl}；业务仓可 {@code @Primary} 装饰或覆盖。
 */
public interface OpenPlatformService {

    // --- Account ---

    OpenAccountSnapshot getOrCreateAccount(String userUuid, String name);

    OpenAccountSnapshot getAccountByUser(String userUuid);

    OpenAccountSnapshot requireActiveAccount(String accountUuid);

    // --- App ---

    OpenAppSnapshot getApp(String appId);

    OpenAppSnapshot requireActiveApp(String appId);

    List<OpenAppSnapshot> listAppsByAccount(String accountUuid);

    OpenAppRegisterOutcome registerApp(String accountUuid, String name, String redirectUri, String scope, OpenAppType appType);

    OpenAppSnapshot updateApp(String accountUuid, String appId, String name, String redirectUri, String scope, OpenAppType appType);

    OpenAppSnapshot updateAppStatus(String appId, int status);

    OpenAppRegisterOutcome resetAppSecret(String accountUuid, String appId);

    boolean validateAppSecret(String appId, String plainSecret);

    void validateRedirectUri(String appId, String redirectUri);

    // --- Identity ---

    OpenIdentitySnapshot resolveIdentity(String appId, String userUuid);

    OpenIdentitySnapshot getIdentity(String appId, String userUuid);

    OpenIdentitySnapshot getIdentityByOpenId(String openId);

    // --- OAuth ---

    OpenUserInfoSnapshot buildUserInfo(String accessToken);

    OpenTokenSnapshot issueTokenFromCode(String appId, String code, String redirectUri);

    OpenTokenSnapshot refreshToken(String appId, String refreshToken);
}
