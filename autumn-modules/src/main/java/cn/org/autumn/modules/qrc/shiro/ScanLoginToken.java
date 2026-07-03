package cn.org.autumn.modules.qrc.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

public class ScanLoginToken extends UsernamePasswordToken {
    public ScanLoginToken(String exchangeToken) {
        super(exchangeToken, "");
    }
}
