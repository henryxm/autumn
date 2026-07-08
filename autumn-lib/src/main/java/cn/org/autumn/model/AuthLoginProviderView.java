package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** 登录页统一授权 Provider 视图（经典 / 开放共用外壳，专有字段按 {@link #type} 区分）。 */
@Getter
@Setter
public class AuthLoginProviderView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String id;
    private String name;
    private String iconUrl;
    private int sortOrder;
    /** 相对路径，不含 callback（前端安全拼接）。 */
    private String loginUrl;

    /** oauth2_classic 专有 */
    private String clientId;
    private Boolean sameInstance;

    /** oauth2_open 专有 */
    private String appId;
    private String platformBaseUrl;
}
