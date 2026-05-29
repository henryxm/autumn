package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayGestureSetRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int[] points;
    private int[] confirmPoints;
}
