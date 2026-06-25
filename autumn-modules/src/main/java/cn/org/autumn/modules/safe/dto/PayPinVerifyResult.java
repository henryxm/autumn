package cn.org.autumn.modules.safe.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayPinVerifyResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String verifyToken;
}
