package cn.org.autumn.modules.safe.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayGestureChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int[] oldPoints;
    private int[] newPoints;
    private int[] confirmPoints;
}
