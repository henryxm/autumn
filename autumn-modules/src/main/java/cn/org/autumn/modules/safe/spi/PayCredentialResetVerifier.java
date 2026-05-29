package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.exception.CodeException;

/**
 * 支付密码/手势重置前的身份校验扩展点。
 */
public interface PayCredentialResetVerifier {

    boolean supports(PayResetContext ctx);

    void verifyReset(PayResetContext ctx) throws CodeException;
}
