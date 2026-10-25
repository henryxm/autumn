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
    public static final String DEFAULT_COMMUNITY_PATH = "/user/community.html?product=chaoranxin";

    private String privacyUrl = DEFAULT_PRIVACY_PATH;
    private String termsUrl = DEFAULT_TERMS_PATH;
    private String aboutUrl = "";
    private String helpUrl = "";
    private String contactUrl = "";
    private String communityUrl = DEFAULT_COMMUNITY_PATH;
}
