package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.ConnectBindResolveResult;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.dto.OpcUserInfoResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.service.UserProfileService;
import org.apache.commons.lang3.StringUtils;
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

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult completeOAuthCallback(ConnectAppEntity app, String code) {
        OpcTokenResult token = connectOauthService.exchangeCode(app, code);
        OpcUserInfoResult userInfoResult = connectOauthService.fetchUserInfoForBind(app, token.getAccessToken());
        if (userInfoResult == null || userInfoResult.getSnapshot() == null || StringUtils.isBlank(userInfoResult.getSnapshot().getOpenId())) {
            throw ConnectBindException.invalidUserInfo(app);
        }
        ConnectBindResolveResult result = connectBindService.resolveAndBind(app, userInfoResult.getSnapshot(), userInfoResult.getPlatformUser());
        userProfileService.establishSession(result.getProfile());
        return result;
    }
}
