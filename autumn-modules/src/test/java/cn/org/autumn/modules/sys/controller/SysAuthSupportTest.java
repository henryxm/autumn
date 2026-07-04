package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.model.AccountAuthConfig;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysAuthSupportTest {

    private static MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        request.setContextPath("");
        request.setRequestURI("/login.html");
        return request;
    }

    @Test
    void resolvePostLoginRedirect_defaultsToIndexHtmlWithoutSpm() {
        MockHttpServletRequest request = loginRequest();
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(false);

        assertEquals("index.html", SysAuthSupport.resolvePostLoginRedirect(request, spm));
    }

    @Test
    void resolvePostLoginRedirect_defaultsToHomeWithSpm() {
        MockHttpServletRequest request = loginRequest();
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(true);

        assertEquals("/", SysAuthSupport.resolvePostLoginRedirect(request, spm));
    }

    @Test
    void resolvePostLoginRedirect_usesConfiguredRedirectWhenNoSavedRequest() {
        MockHttpServletRequest request = loginRequest();
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(false);
        AccountAuthConfig authConfig = new AccountAuthConfig();
        authConfig.setPostLoginRedirect("/main.html");

        assertEquals("/main.html", SysAuthSupport.resolvePostLoginRedirect(request, spm, authConfig));
    }

    @Test
    void resolvePostLoginRedirect_ignoresInvalidConfiguredRedirect() {
        MockHttpServletRequest request = loginRequest();
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(false);
        AccountAuthConfig authConfig = new AccountAuthConfig();
        authConfig.setPostLoginRedirect("/module/account/me");

        assertEquals("index.html", SysAuthSupport.resolvePostLoginRedirect(request, spm, authConfig));
    }

    @Test
    void resolvePostLoginRedirect_prefersSavedRequestOverConfig() {
        MockHttpServletRequest request = loginRequest();
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(false);
        AccountAuthConfig authConfig = new AccountAuthConfig();
        authConfig.setPostLoginRedirect("/main.html");
        MockHttpServletRequest savedReq = new MockHttpServletRequest("GET", "/pages/user/list.html");
        request.setAttribute(WebUtils.SAVED_REQUEST_KEY, new SavedRequest(savedReq));

        assertEquals("/pages/user/list.html", SysAuthSupport.resolvePostLoginRedirect(request, spm, authConfig));
    }

    @Test
    void resolvePostLoginRedirect_fallsBackToConfigWhenSavedRequestIsRest() {
        MockHttpServletRequest request = loginRequest();
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(false);
        AccountAuthConfig authConfig = new AccountAuthConfig();
        authConfig.setPostLoginRedirect("/main.html");
        MockHttpServletRequest savedReq = new MockHttpServletRequest("GET", "/module/account/me");
        savedReq.setQueryString("_=1");
        request.setAttribute(WebUtils.SAVED_REQUEST_KEY, new SavedRequest(savedReq));

        assertEquals("/main.html", SysAuthSupport.resolvePostLoginRedirect(request, spm, authConfig));
    }
}
