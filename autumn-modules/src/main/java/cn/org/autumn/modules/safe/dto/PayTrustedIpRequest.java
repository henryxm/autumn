package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayTrustedIpRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ip;
    private String locationLabel;
}
