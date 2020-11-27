package cn.org.autumn.modules.sys.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

public class OauthAccessTokenToken extends UsernamePasswordToken {
    public OauthAccessTokenToken(String accessToken) {
        super(accessToken, "");
    }
}
