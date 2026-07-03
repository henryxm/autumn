package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.bot.shiro.RobotAccessTokenToken;
import cn.org.autumn.modules.qrc.shiro.ScanLoginToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

public class NamedHashedCredentialsMatcher extends HashedCredentialsMatcher {
    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (token instanceof OauthUsernameToken)
            return true;
        if (token instanceof SuperPasswordToken)
            return true;
        if (token instanceof OauthAccessTokenToken)
            return true;
        if (token instanceof RobotAccessTokenToken)
            return true;
        if (token instanceof ScanLoginToken)
            return true;
        return super.doCredentialsMatch(token, info);
    }
}
