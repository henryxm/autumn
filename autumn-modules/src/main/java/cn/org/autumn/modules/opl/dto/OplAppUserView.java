package cn.org.autumn.modules.opl.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OplAppUserView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String uuid;
    private String appId;
    private String user;
    private String username;
    private String openId;
    private String unionId;
    private Date create;
}
