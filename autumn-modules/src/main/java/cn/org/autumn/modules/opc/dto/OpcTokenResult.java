package cn.org.autumn.modules.opc.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcTokenResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}
