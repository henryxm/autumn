package cn.org.autumn.modules.safe.dto;

import cn.org.autumn.modules.safe.entity.PayUserTrustedDeviceEntity;
import cn.org.autumn.modules.safe.entity.PayUserTrustedIpEntity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaySecurityStatusResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean gateEnabled;
    private boolean passwordlessEnabled;
    private long passwordlessMaxAmountCent;
    private int passwordlessWindowMinutes;
    private long highAmountThresholdCent;
    private boolean passwordlessWindowActive;
    private long passwordlessRemainingSeconds;
    private boolean gesturePaymentEnabled;
    private List<PayUserTrustedDeviceEntity> trustedDevices = new ArrayList<>();
    private List<PayUserTrustedIpEntity> trustedIps = new ArrayList<>();
}
