package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PaySecuritySettingsUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean passwordlessEnabled;
    private Long passwordlessMaxAmountCent;
    private Integer passwordlessWindowMinutes;
    private Boolean gesturePaymentEnabled;
}
