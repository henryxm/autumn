package cn.org.autumn.modules.opc.dto;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/** 绑定记录技术信息，仅供管理员在管理页排查。 */
@Getter
@Setter
public class OpcBindTechnicalView {

    private String appId;
    private String connectApp;
    private String username;
    private String nickname;
    private String userUuid;
    private String openId;
    private String unionId;
    private Date updatedAt;
}
