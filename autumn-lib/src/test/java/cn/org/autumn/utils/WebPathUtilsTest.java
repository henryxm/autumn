package cn.org.autumn.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebPathUtilsTest {

    private static MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        request.setContextPath("");
        request.setRequestURI("/login");
        request.setServletPath("/login");
        return request;
    }

    @Test
    void safePostLoginRedirect_fallsBackToHomeForRestWithCacheBuster() {
        MockHttpServletRequest request = loginRequest();
        String home = WebPathUtils.forBrowser(request, "/");
        assertEquals(home, WebPathUtils.safePostLoginRedirect(request, "/module/account/me?_=1783134769844"));
    }

    @Test
    void safePostLoginRedirect_fallsBackToHomeForBareRestPath() {
        MockHttpServletRequest request = loginRequest();
        String home = WebPathUtils.forBrowser(request, "/");
        assertEquals(home, WebPathUtils.safePostLoginRedirect(request, "/module/report/summary"));
    }

    @Test
    void safePostLoginRedirect_blocksInfrastructurePaths() {
        MockHttpServletRequest request = loginRequest();
        String home = WebPathUtils.forBrowser(request, "/");
        assertEquals(home, WebPathUtils.safePostLoginRedirect(request, "/actuator/health"));
        assertEquals(home, WebPathUtils.safePostLoginRedirect(request, "/sys/menu/nav"));
    }

    @Test
    void safePostLoginRedirect_allowsSpmPage() {
        MockHttpServletRequest request = loginRequest();
        String target = "/?spm=demo.dashboard";
        assertEquals(WebPathUtils.forBrowser(request, target), WebPathUtils.safePostLoginRedirect(request, target));
    }

    @Test
    void safePostLoginRedirect_allowsIndexHtml() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("/index.html", WebPathUtils.safePostLoginRedirect(request, "/index.html"));
    }

    @Test
    void safePostLoginRedirect_allowsOpenPlatformAuthorizePage() {
        MockHttpServletRequest request = loginRequest();
        String target = "/open/oauth2/authorize?app_id=ax7f8da3af683d45ba&redirect_uri=http%3A%2F%2Fexample%2Fcb&response_type=code&scope=basic&state=s1";
        assertEquals(target, WebPathUtils.safePostLoginRedirect(request, target));
    }

    @Test
    void safeOauthCallbackForClient_allowsOpenPlatformAuthorizePage() {
        MockHttpServletRequest request = loginRequest();
        String target = "/open/oauth2/authorize?app_id=test&redirect_uri=http%3A%2F%2Fexample%2Fcb&response_type=code";
        assertEquals(target, WebPathUtils.safeOauthCallbackForClient(request, target));
    }

    @Test
    void safePostLoginRedirect_respectsContextPath() {
        MockHttpServletRequest request = loginRequest();
        request.setContextPath("/app");
        request.setRequestURI("/app/index.html");
        assertEquals("/app/index.html", WebPathUtils.safePostLoginRedirect(request, "/app/index.html"));
    }

    @Test
    void shouldPersistSavedRequest_rejectsApiAjaxAndRest() {
        MockHttpServletRequest json = new MockHttpServletRequest("GET", "/module/account/me");
        json.addHeader("Accept", "application/json");
        assertFalse(WebPathUtils.shouldPersistSavedRequest(json));

        MockHttpServletRequest rest = new MockHttpServletRequest("GET", "/module/account/me");
        rest.addHeader("Accept", "text/html,application/xhtml+xml");
        assertFalse(WebPathUtils.shouldPersistSavedRequest(rest));

        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/user/profile");
        assertFalse(WebPathUtils.shouldPersistSavedRequest(api));
    }

    @Test
    void shouldPersistSavedRequest_allowsDocumentNavigation() {
        MockHttpServletRequest index = new MockHttpServletRequest("GET", "/index.html");
        index.addHeader("Accept", "text/html,application/xhtml+xml");
        assertTrue(WebPathUtils.shouldPersistSavedRequest(index));

        MockHttpServletRequest spm = new MockHttpServletRequest("GET", "/");
        spm.setQueryString("spm=demo.portal");
        spm.addHeader("Accept", "text/html,application/xhtml+xml");
        assertTrue(WebPathUtils.shouldPersistSavedRequest(spm));
    }

    @Test
    void requestPathWithoutContext_stripsContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/index.html");
        request.setContextPath("/app");
        request.setRequestURI("/app/index.html");
        assertEquals("/index.html", WebPathUtils.requestPathWithoutContext(request));
    }

    @Test
    void safeOauthCallbackForClient_stripsUnsafeRestTarget() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("", WebPathUtils.safeOauthCallbackForClient(request, "/module/account/me?_=1783134769844"));
        assertEquals("", WebPathUtils.safeOauthCallbackForClient(request, "/module/report/summary"));
    }

    @Test
    void safeOauthCallbackForClient_allowsDocumentTargets() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("/index.html", WebPathUtils.safeOauthCallbackForClient(request, "/index.html"));
        assertEquals("/?spm=demo.portal", WebPathUtils.safeOauthCallbackForClient(request, "/?spm=demo.portal"));
        assertEquals(WebPathUtils.forBrowser(request, "/"), WebPathUtils.safeOauthCallbackForClient(request, "/"));
    }

    @Test
    void resolveLoginRedirectWithSavedRequest_prefersSafeDocumentPage() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("/index.html", WebPathUtils.resolveLoginRedirectWithSavedRequest(request, "/index.html", "index.html"));
        assertEquals("/?spm=demo.portal", WebPathUtils.resolveLoginRedirectWithSavedRequest(request, "/?spm=demo.portal", "index.html"));
    }

    @Test
    void resolveLoginRedirectWithSavedRequest_fallsBackWhenSavedIsRest() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("index.html", WebPathUtils.resolveLoginRedirectWithSavedRequest(request, "/module/account/me?_=1", "index.html"));
    }

    @Test
    void configuredPostLoginFallback_usesValidConfig() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("/main.html", WebPathUtils.configuredPostLoginFallback(request, "/main.html", "index.html"));
        assertEquals("index.html", WebPathUtils.configuredPostLoginFallback(request, "index.html", "fallback.html"));
    }

    @Test
    void configuredPostLoginFallback_rejectsInvalidConfig() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("index.html", WebPathUtils.configuredPostLoginFallback(request, "/module/account/me", "index.html"));
        assertEquals("index.html", WebPathUtils.configuredPostLoginFallback(request, "/sys/menu/nav", "index.html"));
    }

    @Test
    void configuredPostLoginFallback_allowsExplicitHome() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("/", WebPathUtils.configuredPostLoginFallback(request, "/", "index.html"));
    }

    @Test
    void configuredPostLoginFallback_blankUsesSystemDefault() {
        MockHttpServletRequest request = loginRequest();
        assertEquals("index.html", WebPathUtils.configuredPostLoginFallback(request, null, "index.html"));
        assertEquals("index.html", WebPathUtils.configuredPostLoginFallback(request, "  ", "index.html"));
    }

    @Test
    void isValidPostLoginRedirectConfig_acceptsDocumentPaths() {
        assertTrue(WebPathUtils.isValidPostLoginRedirectConfig("/index.html"));
        assertTrue(WebPathUtils.isValidPostLoginRedirectConfig("main.html"));
        assertTrue(WebPathUtils.isValidPostLoginRedirectConfig("/?spm=demo.portal"));
        assertTrue(WebPathUtils.isValidPostLoginRedirectConfig("/"));
    }

    @Test
    void isValidPostLoginRedirectConfig_rejectsRestPaths() {
        assertFalse(WebPathUtils.isValidPostLoginRedirectConfig("/module/account/me"));
        assertFalse(WebPathUtils.isValidPostLoginRedirectConfig("/sys/menu/nav"));
        assertFalse(WebPathUtils.isValidPostLoginRedirectConfig("/actuator/health"));
    }

    @Test
    void canonicalOauthLoginCallback_unwrapsNestedOauthLoginEntry() throws Exception {
        MockHttpServletRequest request = loginRequest();
        String inner = URLEncoder.encode("http://localhost/login", StandardCharsets.UTF_8.name());
        String nested = "http://localhost/oauth2/login?client_id=demo&callback=" + inner;
        assertEquals("http://localhost/login", WebPathUtils.canonicalOauthLoginCallback(request, nested));
    }

    @Test
    void canonicalOauthLoginCallback_deepNestedFallsBackToLoginPage() throws Exception {
        MockHttpServletRequest request = loginRequest();
        String level1 = URLEncoder.encode("/login", StandardCharsets.UTF_8.name());
        String level2 = URLEncoder.encode("http://localhost/oauth2/login?client_id=demo&callback=" + level1, StandardCharsets.UTF_8.name());
        String nested = "http://localhost/oauth2/login?client_id=demo&callback=" + level2;
        assertEquals("/login", WebPathUtils.canonicalOauthLoginCallback(request, nested));
    }

    @Test
    void oauthLoginEntryUrlIfCallbackNeedsCanonical_returnsRedirectWhenNested() throws Exception {
        MockHttpServletRequest request = loginRequest();
        String inner = URLEncoder.encode("/login", StandardCharsets.UTF_8.name());
        String nested = "http://localhost/oauth2/login?client_id=demo&callback=" + inner;
        String url = WebPathUtils.oauthLoginEntryUrlIfCallbackNeedsCanonical(request, "/oauth2/login", "client_id", "demo", nested);
        assertEquals("/oauth2/login?client_id=demo&callback=%2Flogin", url);
    }

    @Test
    void isSameSiteUrl_blankOriginIsSameInstance() {
        assertTrue(WebPathUtils.isSameSiteUrl(null, "http://local.example.com"));
        assertTrue(WebPathUtils.isSameSiteUrl("  ", "http://local.example.com"));
    }

    @Test
    void isSameSiteUrl_matchesIgnoringCaseAndTrailingSlash() {
        assertTrue(WebPathUtils.isSameSiteUrl("http://Local.Example.com/", "http://local.example.com"));
        assertTrue(WebPathUtils.isSameSiteUrl("http://local.example.com", "http://local.example.com/"));
    }

    @Test
    void isSameSiteUrl_rejectsDifferentHosts() {
        assertFalse(WebPathUtils.isSameSiteUrl("http://remote.example.com", "http://local.example.com"));
        assertFalse(WebPathUtils.isSameSiteUrl("http://local.example.com", null));
    }
}
