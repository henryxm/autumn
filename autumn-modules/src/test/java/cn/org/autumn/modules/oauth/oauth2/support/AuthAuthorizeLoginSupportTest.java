package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthAuthorizeLoginSupportTest {

    @Test
    void isAuthorizeCallback_detectsOAuthAndOplPaths() {
        assertTrue(AuthAuthorizeLoginSupport.isAuthorizeCallback("https://as.example.com/oauth2/authorize?client_id=c1"));
        assertTrue(AuthAuthorizeLoginSupport.isAuthorizeCallback("/open/oauth2/authorize?app_id=a1"));
        assertFalse(AuthAuthorizeLoginSupport.isAuthorizeCallback("/login"));
    }

    @Test
    void saveAndResolveLoginTab_usesSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession(null));
        AuthAuthorizeLoginSupport.saveLoginTab(request, "qr");
        assertEquals("qr", AuthAuthorizeLoginSupport.resolveLoginTab(request));
    }

    @Test
    void resolveLoginTab_prefersSessionOverQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession(null));
        AuthAuthorizeLoginSupport.saveLoginTab(request, "phone");
        request.setParameter("loginTab", "account");
        assertEquals("phone", AuthAuthorizeLoginSupport.resolveLoginTab(request));
    }

    @Test
    void appendLoginTab_addsQueryParam() {
        assertEquals("/oauth2/authorize?client_id=c&loginTab=qr",
                AuthAuthorizeLoginSupport.appendLoginTab("/oauth2/authorize?client_id=c", "qr"));
    }

    @Test
    void resolveLoginTabForLogin_detectsPhoneUsername() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("loginTab", "phone");
        assertEquals("phone", AuthAuthorizeLoginSupport.resolveLoginTabForLogin(request, "13800138000"));
        assertEquals("phone", AuthAuthorizeLoginSupport.resolveLoginTabForLogin(new MockHttpServletRequest(), "13800138000"));
        assertEquals("account", AuthAuthorizeLoginSupport.resolveLoginTabForLogin(new MockHttpServletRequest(), "admin"));
    }
}
