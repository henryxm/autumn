package cn.org.autumn.modules.sys.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

public class UserContextServiceOpenApiTest {

    @Test
    public void openPlatformApiPathDetected() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/open/api/v1/platform/app/save");
        assertTrue(UserContextService.isOpenPlatformApiRequest(new ServletWebRequest(request)));
    }

    @Test
    public void nonOpenApiPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bot/api/v1/message");
        assertFalse(UserContextService.isOpenPlatformApiRequest(new ServletWebRequest(request)));
    }

    @Test
    public void hintOnlyRejectedOnOpenPlatformApi() {
        UserContextService service = new UserContextService();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/open/api/v1/platform/app/list");
        request.addParameter("userUuid", "00000000000000000000000000000001");
        assertTrue(UserContextService.isOpenPlatformApiRequest(new ServletWebRequest(request, new MockHttpServletResponse())));
    }
}
