package cn.org.autumn.site;

import cn.org.autumn.config.SiteLegalLinksHandler;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.model.SitePortalLegalLinks;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class SiteLegalLinksFactory extends Factory {

    public SitePortalLegalLinks resolve(SitePortalConfig config) {
        SitePortalLegalLinks base = config == null || config.getLegalLinks() == null ? new SitePortalLegalLinks() : config.getLegalLinks();
        SitePortalLegalLinks resolved = new SitePortalLegalLinks();
        resolved.setPrivacyUrl(firstNonBlank(config, "privacyUrl", base.getPrivacyUrl()));
        resolved.setTermsUrl(firstNonBlank(config, "termsUrl", base.getTermsUrl()));
        resolved.setAboutUrl(firstNonBlank(config, "aboutUrl", base.getAboutUrl()));
        resolved.setHelpUrl(firstNonBlank(config, "helpUrl", base.getHelpUrl()));
        resolved.setContactUrl(firstNonBlank(config, "contactUrl", base.getContactUrl()));
        return resolved;
    }

    private String firstNonBlank(SitePortalConfig config, String method, String fallback) {
        List<SiteLegalLinksHandler> handlers = getOrderList(SiteLegalLinksHandler.class);
        if (handlers != null) {
            for (SiteLegalLinksHandler handler : handlers) {
                if (handler instanceof SiteLegalLinksFactory) {
                    continue;
                }
                String value = invokeHandler(handler, config, method);
                if (StringUtils.isNotBlank(value)) {
                    return value.trim();
                }
            }
        }
        return StringUtils.defaultIfBlank(fallback, "");
    }

    private String invokeHandler(SiteLegalLinksHandler handler, SitePortalConfig config, String method) {
        try {
            switch (method) {
                case "privacyUrl":
                    return handler.privacyUrl(config);
                case "termsUrl":
                    return handler.termsUrl(config);
                case "aboutUrl":
                    return handler.aboutUrl(config);
                case "helpUrl":
                    return handler.helpUrl(config);
                case "contactUrl":
                    return handler.contactUrl(config);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
