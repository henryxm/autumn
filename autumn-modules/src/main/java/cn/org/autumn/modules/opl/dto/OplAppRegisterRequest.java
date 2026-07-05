package cn.org.autumn.modules.opl.dto;

import cn.org.autumn.opl.model.OpenAppType;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OplAppRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String redirectUri;
    private String scope;
    private OpenAppType appType;
}
