package cn.org.autumn.config;

import cn.org.autumn.model.SitePortalConfig;

/**
 * 下游子项目可覆盖登录/注册页法律链接（隐私、服务条款、关于我们等）。
 */
public interface SiteLegalLinksHandler {

    default String privacyUrl(SitePortalConfig config) {
        return null;
    }

    default String termsUrl(SitePortalConfig config) {
        return null;
    }

    default String aboutUrl(SitePortalConfig config) {
        return null;
    }

    default String helpUrl(SitePortalConfig config) {
        return null;
    }

    default String contactUrl(SitePortalConfig config) {
        return null;
    }
}
