package cn.org.autumn.site;

import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthPageAttributesTest {

    @Test
    void applySafeOauthCallback_stripsUnsafeCallbackParam() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        request.setParameter("callback", "/module/account/me?_=1783134769844");
        Model model = new ExtendedModelMap();

        AuthPageAttributes.applySafeOauthCallback(request, model);

        assertEquals("", model.getAttribute("safeOauthCallback"));
    }

    @Test
    void applySafeOauthCallback_allowsSpmCallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        request.setParameter("callback", "/?spm=demo.portal");
        Model model = new ExtendedModelMap();

        AuthPageAttributes.applySafeOauthCallback(request, model);

        assertEquals("/?spm=demo.portal", model.getAttribute("safeOauthCallback"));
    }

    @Test
    void applySafeOauthCallback_usesAuthorizePageUrlWhenOauthAuthorize() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setQueryString("client_id=demo&response_type=code");
        request.setRequestURI("/oauth2/authorize");
        Model model = new ExtendedModelMap();
        model.addAttribute("oauthAuthorize", true);

        AuthPageAttributes.applySafeOauthCallback(request, model);

        assertEquals("http://localhost/oauth2/authorize?client_id=demo&response_type=code", model.getAttribute("safeOauthCallback"));
    }

    @Test
    void applySafeOauthCallback_readsCallbackFromReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        request.addHeader("referer", "http://localhost/oauth2/login?callback=%2Findex.html");
        Model model = new ExtendedModelMap();

        AuthPageAttributes.applySafeOauthCallback(request, model);

        assertEquals("/index.html", model.getAttribute("safeOauthCallback"));
    }

    @Test
    void apply_populatesSiteAttributes() {
        SysConfigService config = mock(SysConfigService.class);
        when(config.getLoadingBrand()).thenReturn("Autumn");
        when(config.isRegisterEnabled()).thenReturn(true);
        when(config.isForgotPasswordEnabled()).thenReturn(false);
        Model model = new ExtendedModelMap();

        AuthPageAttributes.apply(model, config);

        assertEquals("Autumn", model.getAttribute("siteName"));
        assertEquals(true, model.getAttribute("registerEnabled"));
        assertEquals(false, model.getAttribute("forgotPasswordEnabled"));
    }
}
