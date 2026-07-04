package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.utils.WebPathUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysAuthSupportTest {

    @Test
    void resolvePostLoginRedirect_defaultsToIndexHtmlWithoutSpm() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(false);

        assertEquals("index.html", SysAuthSupport.resolvePostLoginRedirect(request, spm));
    }

    @Test
    void resolvePostLoginRedirect_defaultsToHomeWithSpm() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        SuperPositionModelService spm = mock(SuperPositionModelService.class);
        when(spm.menuWithSpm()).thenReturn(true);

        assertEquals("/", SysAuthSupport.resolvePostLoginRedirect(request, spm));
    }
}
