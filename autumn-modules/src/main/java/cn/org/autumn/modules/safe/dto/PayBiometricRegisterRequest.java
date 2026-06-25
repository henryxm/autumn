package cn.org.autumn.modules.safe.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayBiometricRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String platform;
    private String credentialId;
    private String publicKey;
}
