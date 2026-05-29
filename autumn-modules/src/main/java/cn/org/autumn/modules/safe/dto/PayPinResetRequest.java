package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayPinResetRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String newPin;
    private String confirm;
    private String loginPassword;
    private String smsCode;
}
