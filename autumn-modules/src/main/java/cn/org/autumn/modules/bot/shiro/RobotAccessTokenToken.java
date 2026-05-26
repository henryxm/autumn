package cn.org.autumn.modules.bot.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

public class RobotAccessTokenToken extends UsernamePasswordToken {
    public RobotAccessTokenToken(String accessToken) {
        super(accessToken, "");
    }
}
