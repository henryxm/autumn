package cn.org.autumn.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpNavigationUtilsTest {

    @Test
    void isApiOrAjaxRequest_detectsJsonAcceptAndXhr() {
        MockHttpServletRequest jsonAccept = new MockHttpServletRequest("GET", "/module/account/me");
        jsonAccept.addHeader("Accept", "application/json");
        assertTrue(HttpNavigationUtils.isApiOrAjaxRequest(jsonAccept));

        MockHttpServletRequest ajax = new MockHttpServletRequest("GET", "/foo");
        ajax.addHeader("X-Requested-With", "XMLHttpRequest");
        assertTrue(HttpNavigationUtils.isApiOrAjaxRequest(ajax));

        MockHttpServletRequest apiPath = new MockHttpServletRequest("GET", "/api/user/profile");
        assertTrue(HttpNavigationUtils.isApiOrAjaxRequest(apiPath));

        MockHttpServletRequest page = new MockHttpServletRequest("GET", "/index.html");
        page.addHeader("Accept", "text/html,application/xhtml+xml");
        assertFalse(HttpNavigationUtils.isApiOrAjaxRequest(page));
    }

    @Test
    void isLikelyBrowserDocumentUrl_allowsHtmlSpmAndRoot() {
        assertTrue(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/", null));
        assertTrue(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/index.html", null));
        assertTrue(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/", "spm=demo.portal"));
        assertTrue(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/oauth2/authorize", "client_id=x"));
        assertTrue(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/open/oauth2/authorize", "app_id=x"));
    }

    @Test
    void isOauthAuthorizeConsentPath_coversAsAndOpenPlatformAuthorize() {
        assertTrue(HttpNavigationUtils.isOauthAuthorizeConsentPath("/oauth2/authorize"));
        assertTrue(HttpNavigationUtils.isOauthAuthorizeConsentPath("/open/oauth2/authorize"));
        assertFalse(HttpNavigationUtils.isOauthAuthorizeConsentPath("/oauth2/token"));
    }

    @Test
    void isLikelyBrowserDocumentUrl_rejectsRestAndAjaxCacheBuster() {
        assertFalse(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/module/account/me", "_=1783134769844"));
        assertFalse(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/module/report/summary", null));
        assertFalse(HttpNavigationUtils.isLikelyBrowserDocumentUrl("/module/foo/list", "_t=123"));
    }

    @Test
    void isInfrastructureApiPath_blocksFrameworkEndpoints() {
        assertTrue(HttpNavigationUtils.isInfrastructureApiPath("/actuator/health"));
        assertTrue(HttpNavigationUtils.isInfrastructureApiPath("/sys/menu/nav"));
        assertTrue(HttpNavigationUtils.isInfrastructureApiPath("/oauth2/token"));
        assertFalse(HttpNavigationUtils.isInfrastructureApiPath("/index.html"));
    }

    @Test
    void isAjaxCacheBusterOnlyQuery() {
        assertTrue(HttpNavigationUtils.isAjaxCacheBusterOnlyQuery("_=1783134769844"));
        assertTrue(HttpNavigationUtils.isAjaxCacheBusterOnlyQuery("_t=99"));
        assertFalse(HttpNavigationUtils.isAjaxCacheBusterOnlyQuery("spm=demo.portal&_=1"));
    }
}
