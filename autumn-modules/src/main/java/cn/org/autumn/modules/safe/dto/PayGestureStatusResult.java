package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayGestureStatusResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean set;
    private boolean locked;
    private int remainingAttempts;
}
