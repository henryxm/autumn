package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@ConfigParam(paramKey = AuthSiteConfig.CONFIG_KEY, category = AuthSiteConfig.config, name = "认证站点角色配置", description = "AS/RP 双角色扫码与 OAuth 联邦登录")
public class AuthSiteConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "AUTH_SITE_CONFIG";
    public static final String config = "auth_site_config";

    public static final String ROLE_AS_ONLY = "AS_ONLY";
    public static final String ROLE_RP_ONLY = "RP_ONLY";
    public static final String ROLE_AS_AND_RP = "AS_AND_RP";

    @ConfigField(category = InputType.SelectionType, name = "站点认证角色", description = "AS_ONLY=仅身份源；RP_ONLY=仅依赖方；AS_AND_RP=同源 AS+RP", options = "AS_ONLY,RP_ONLY,AS_AND_RP")
    private String siteRole = ROLE_AS_AND_RP;

    @ConfigField(category = InputType.SelectionType, name = "RP 扫码前端模式", description = "as=同源 /qrc/scanticket/web；rp=联邦 /client/oauth2/qrc/web；auto=按站点角色自动", options = "auto,as,rp")
    private String qrcWebMode = "auto";

    public String normalizedSiteRole() {
        if (StringUtils.isBlank(siteRole)) {
            return ROLE_AS_AND_RP;
        }
        String value = siteRole.trim().toUpperCase();
        if (ROLE_AS_ONLY.equals(value) || ROLE_RP_ONLY.equals(value) || ROLE_AS_AND_RP.equals(value)) {
            return value;
        }
        return ROLE_AS_AND_RP;
    }

    public String normalizedQrcWebMode() {
        if (StringUtils.isBlank(qrcWebMode)) {
            return "auto";
        }
        String value = qrcWebMode.trim().toLowerCase();
        if ("as".equals(value) || "rp".equals(value) || "auto".equals(value)) {
            return value;
        }
        return "auto";
    }
}
