package cn.org.autumn.modules.sys.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

public class OauthUsernameToken extends UsernamePasswordToken {
    public OauthUsernameToken(String username) {
        super(username, "");
    }
}
