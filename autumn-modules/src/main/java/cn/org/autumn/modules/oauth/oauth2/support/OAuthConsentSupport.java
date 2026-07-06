package cn.org.autumn.modules.oauth.oauth2.support;

/**
 * OAuth2 授权确认勾选校验（OAuth / OPL 共用）。
 */
public final class OAuthConsentSupport {

    private OAuthConsentSupport() {
    }

    public static boolean consented(String userConsent) {
        return "true".equalsIgnoreCase(userConsent) || "1".equals(userConsent) || "on".equalsIgnoreCase(userConsent);
    }
}
