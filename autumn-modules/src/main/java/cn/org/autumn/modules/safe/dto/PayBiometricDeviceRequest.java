package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayBiometricDeviceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceId;
}
