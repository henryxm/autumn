package cn.org.autumn.modules.sys.shiro;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

public class OauthHashedCredentialsMatcher extends HashedCredentialsMatcher {
    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (token instanceof OauthUsernameToken)
            return true;
        return super.doCredentialsMatch(token, info);
    }
}
