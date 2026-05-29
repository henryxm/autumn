package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayTrustedDeviceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String deviceName;
    private String platform;
}
