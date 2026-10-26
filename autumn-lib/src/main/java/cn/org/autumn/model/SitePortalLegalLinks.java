package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SitePortalLegalLinks implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_PRIVACY_PATH = "/user/privacy.html";
    public static final String DEFAULT_TERMS_PATH = "/user/service.html";
    public static final String DEFAULT_ABOUT_PATH = "/user/about.html";
    /** 框架默认不含业务 product；带 product 的 URL 由业务模块 {@code SiteLegalLinksHandler#communityUrl} 或门户配置提供。 */
    public static final String DEFAULT_COMMUNITY_PATH = "/user/community.html";

    private String privacyUrl = DEFAULT_PRIVACY_PATH;
    private String termsUrl = DEFAULT_TERMS_PATH;
    private String aboutUrl = "";
    private String helpUrl = "";
    private String contactUrl = "";
    private String communityUrl = DEFAULT_COMMUNITY_PATH;
}
