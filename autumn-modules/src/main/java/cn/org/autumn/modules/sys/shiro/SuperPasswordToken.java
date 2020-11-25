package cn.org.autumn.modules.sys.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

public class SuperPasswordToken extends UsernamePasswordToken {
    public SuperPasswordToken(String username) {
        super(username, "");
    }
}
