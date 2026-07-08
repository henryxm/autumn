package cn.org.autumn.modules.opc.dto;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/** 接入绑定管理页展示模型（用户友好字段；技术细节仅管理员可见）。 */
@Getter
@Setter
public class OpcBindManageView {

    private Long id;
    private String appName;
    private String appIcon;
    /** 展示用账号名：优先昵称，其次登录名 */
    private String accountLabel;
    private Date boundAt;
    /** 仅系统管理员返回 */
    private OpcBindTechnicalView technical;
}
