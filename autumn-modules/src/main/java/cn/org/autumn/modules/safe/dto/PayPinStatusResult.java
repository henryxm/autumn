package cn.org.autumn.modules.safe.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayPinStatusResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean set;
    private boolean locked;
    private int remainingAttempts;
}
