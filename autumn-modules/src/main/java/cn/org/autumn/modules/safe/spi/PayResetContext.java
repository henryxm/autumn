package cn.org.autumn.modules.safe.spi;

import lombok.Getter;
import lombok.Setter;

/**
 * 支付密码重置身份校验上下文。
 */
@Getter
@Setter
public class PayResetContext {

    private String userUuid;
    private String loginPassword;
    private String smsCode;
    private String captcha;
    private String captchaUuid;
}
