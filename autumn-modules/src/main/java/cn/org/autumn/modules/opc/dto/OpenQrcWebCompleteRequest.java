package cn.org.autumn.modules.opc.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenQrcWebCompleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String id;
    private String appId;
    private String code;
    private String callback;
}
