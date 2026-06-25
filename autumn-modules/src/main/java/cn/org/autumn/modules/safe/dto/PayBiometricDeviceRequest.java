package cn.org.autumn.modules.safe.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayBiometricDeviceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceId;
}
