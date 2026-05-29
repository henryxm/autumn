package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PayGateAssessResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean authorized;
    /** DENIED | PASSWORD_REQUIRED | PASSWORDLESS */
    private String authMode;
    private boolean needPassword;
    private boolean passwordlessEligible;
    private String gateToken;
    private boolean trustedDevice;
    private boolean trustedIp;
    private long effectivePasswordlessMaxCent;
    private int effectivePasswordlessWindowMinutes;
    private List<String> reasons = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    /** 本次支付允许的校验方式：PIN、BIO、可选 GESTURE */
    private List<String> allowedVerifyMethods = new ArrayList<>();
}
