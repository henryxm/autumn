package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OPC OAuth 回调编排：换 token、拉 userInfo、绑定本地用户并登录。 */
@Service
public class ConnectLoginService {

    @Autowired
    private ConnectOauthService connectOauthService;

    @Autowired
    private ConnectBindService connectBindService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserTokenService userTokenService;

    @Transactional(rollbackFor = Exception.class)
    public UserProfile completeOAuthCallback(ConnectAppEntity app, String code) {
        OpcTokenResult token = connectOauthService.exchangeCode(app, code);
        OpenUserInfoSnapshot userInfo = connectOauthService.fetchUserInfo(app, token.getAccessToken());
        UserProfile profile = connectBindService.resolveAndBind(app, userInfo);
        userProfileService.login(profile);
        userTokenService.saveToken(token.getAccessToken());
        return profile;
    }
}
