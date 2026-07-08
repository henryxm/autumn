package cn.org.autumn.modules.opc.dto;

import lombok.Getter;
import lombok.Setter;

/** 绑定管理页应用筛选项（不含内部 uuid）。 */
@Getter
@Setter
public class OpcAppBriefView {

    private String appId;
    private String name;
    /** 管理员手动添加绑定时使用 */
    private String connectApp;
}
