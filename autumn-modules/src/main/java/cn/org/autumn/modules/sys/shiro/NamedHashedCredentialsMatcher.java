package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.bot.shiro.RobotAccessTokenToken;
import cn.org.autumn.modules.qrc.shiro.ScanLoginToken;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
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
        Object credentials = info.getCredentials();
        if (credentials instanceof String && PasswordHashSupport.isPbkdf2((String) credentials)) {
            char[] submitted = token instanceof UsernamePasswordToken
                    ? ((UsernamePasswordToken) token).getPassword()
                    : null;
            if (submitted == null) {
                return false;
            }
            return PasswordHashSupport.matchesPbkdf2(new String(submitted), (String) credentials);
        }
        // 遗留 SHA-256×16：走 Shiro HashedCredentialsMatcher
        boolean ok = super.doCredentialsMatch(token, info);
        // 惰性升级在登录成功后由 SysUserService 处理（此处仅校验）
        return ok;
    }

    /**
     * 从 AuthenticationInfo 取用户实体（若 principal 为 SysUserEntity）。
     */
    public static SysUserEntity principalUser(AuthenticationInfo info) {
        if (info == null || info.getPrincipals() == null) {
            return null;
        }
        Object p = info.getPrincipals().getPrimaryPrincipal();
        return p instanceof SysUserEntity ? (SysUserEntity) p : null;
    }
}
