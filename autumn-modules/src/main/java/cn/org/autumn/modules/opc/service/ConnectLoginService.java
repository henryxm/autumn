package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.ConnectBindPendingContext;
import cn.org.autumn.modules.opc.dto.ConnectBindResolveResult;
import cn.org.autumn.modules.opc.dto.ConnectOAuthFinishResult;
import cn.org.autumn.modules.opc.dto.OpcTokenResult;
import cn.org.autumn.modules.opc.dto.OpcUserInfoResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.opc.support.ConnectBindException.ConflictType;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
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
    private ConnectBindPendingService connectBindPendingService;

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private UserProfileService userProfileService;

    public String bindChoicePageUrl(HttpServletRequest request, String appId, String pendingToken) {
        String url = WebPathUtils.forBrowser(request, "/open/oauth2/bind/choice?token=" + pendingToken);
        if (StringUtils.isNotBlank(appId)) {
            url = url + "&appId=" + appId;
        }
        return url;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult completePendingCreateNew(String pendingToken) {
        return finishPendingBind(pendingToken, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult completePendingBindSession(String pendingToken) {
        return finishPendingBind(pendingToken, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult completeOAuthCallback(HttpServletRequest request, ConnectAppEntity app, String code, String callback) {
        OpcTokenResult token = connectOauthService.exchangeCode(app, code);
        OpcUserInfoResult userInfoResult = connectOauthService.fetchUserInfoForBind(app, token.getAccessToken());
        if (userInfoResult == null || userInfoResult.getSnapshot() == null || StringUtils.isBlank(userInfoResult.getSnapshot().getOpenId())) {
            throw ConnectBindException.invalidUserInfo(app);
        }
        OpenUserInfoSnapshot snapshot = userInfoResult.getSnapshot();
        ConnectBindResolveResult result;
        try {
            result = connectBindService.resolveAndBind(app, snapshot, userInfoResult.getPlatformUser());
        } catch (ConnectBindException e) {
            if (e.getConflictType() == ConflictType.BIND_CHOICE_REQUIRED) {
                String safeCallback = WebPathUtils.safeOauthCallbackForClient(request, callback);
                String pendingToken = connectBindPendingService.save(app, snapshot, token.getAccessToken(), safeCallback);
                throw ConnectBindException.bindChoiceRequired(app, snapshot.getOpenId(), pendingToken);
            }
            throw e;
        }
        userProfileService.establishSession(result.getProfile());
        return result;
    }

    /** 完成 OAuth 登录；绑定冲突时返回 bind choice 页 URL，不抛异常。 */
    public ConnectOAuthFinishResult finishOAuthLogin(HttpServletRequest request, ConnectAppEntity app, String code, String callback) {
        try {
            completeOAuthCallback(request, app, code, callback);
            return ConnectOAuthFinishResult.success(resolvePostLoginRedirect(request, app, callback));
        } catch (ConnectBindException e) {
            if (e.getConflictType() == ConflictType.BIND_CHOICE_REQUIRED && StringUtils.isNotBlank(e.getPendingToken())) {
                return ConnectOAuthFinishResult.bindChoice(bindChoicePageUrl(request, app.getAppId(), e.getPendingToken()));
            }
            throw e;
        }
    }

    public String resolvePostLoginRedirect(HttpServletRequest request, ConnectAppEntity app, String callback) {
        String redirect = WebPathUtils.safeOauthCallbackForClient(request, callback);
        if (StringUtils.isBlank(redirect)) {
            redirect = WebPathUtils.forBrowser(request, OpcConstants.OAUTH2_SUCCESS_PATH);
            if (app != null && StringUtils.isNotBlank(app.getAppId())) {
                redirect = redirect + (redirect.contains("?") ? "&" : "?") + "appId=" + app.getAppId();
            }
        }
        return redirect;
    }

    private ConnectBindResolveResult finishPendingBind(String pendingToken, boolean createNewUser) {
        ConnectBindPendingContext pending = connectBindPendingService.consume(pendingToken);
        if (pending == null || StringUtils.isBlank(pending.getConnectAppUuid()) || StringUtils.isBlank(pending.getUserInfoJson())) {
            throw new IllegalStateException("绑定会话已过期，请重新发起授权");
        }
        ConnectAppEntity app = StringUtils.isNotBlank(pending.getAppId()) ? connectAppService.getByAppId(pending.getAppId()) : null;
        if (app == null && StringUtils.isNotBlank(pending.getConnectAppUuid())) {
            app = connectAppService.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ConnectAppEntity>().eq("uuid", pending.getConnectAppUuid()));
        }
        if (app == null) {
            throw new IllegalStateException("接入应用不存在");
        }
        OpenUserInfoSnapshot userInfo = JSON.parseObject(pending.getUserInfoJson(), OpenUserInfoSnapshot.class);
        if (userInfo == null || StringUtils.isBlank(userInfo.getOpenId())) {
            throw ConnectBindException.invalidUserInfo(app);
        }
        ConnectBindResolveResult result = createNewUser ? connectBindService.bindCreateNewUser(app, userInfo) : connectBindService.bindSessionUser(app, userInfo);
        userProfileService.establishSession(result.getProfile());
        return result;
    }
}
