package cn.org.autumn.modules.safe.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayPinResetRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String newPin;
    private String confirm;
    private String loginPassword;
    private String smsCode;
}
