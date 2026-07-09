package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** 扫码凭证解析视图（不含 secret）。 */
@Getter
@Setter
public class ScanLoginCredentialView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String id;
    private String name;
    private String redirectUri;
    private String originUri;
    private String platformBaseUrl;
    private String qrcMode;
    private String qrcOpenCreateUri;
    private String inboundCallbackUri;
}
