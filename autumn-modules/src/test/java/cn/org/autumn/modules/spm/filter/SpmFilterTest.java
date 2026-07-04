package cn.org.autumn.modules.spm.filter;

import cn.org.autumn.utils.WebPathUtils;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpmFilterTest {

    @Test
    void onAccessDenied_returns401JsonForApiRequest() throws Exception {
        SpmFilter filter = new SpmFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/module/account/me");
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continued = filter.onAccessDenied(request, response);

        assertFalse(continued);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("401"));
    }

    @Test
    void saveRequest_skipsRestEndpointWithoutJsonHeaders() {
        SavingProbeFilter filter = new SavingProbeFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/module/account/me");
        request.addHeader("Accept", "text/html,application/xhtml+xml");

        filter.saveRequest(request);

        assertFalse(filter.superSaveInvoked);
    }

    @Test
    void saveRequest_allowsIndexHtmlNavigation() {
        SavingProbeFilter filter = new SavingProbeFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
        request.addHeader("Accept", "text/html,application/xhtml+xml");

        filter.saveRequest(request);

        assertTrue(filter.superSaveInvoked);
    }

    @Test
    void issueSuccessRedirect_sanitizesUnsafeSavedRequest() throws Exception {
        SpmFilter filter = new SpmFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest savedReq = new MockHttpServletRequest("GET", "/module/account/me");
        savedReq.setQueryString("_=1");
        SavedRequest saved = new SavedRequest(savedReq);
        request.setAttribute(WebUtils.SAVED_REQUEST_KEY, saved);

        ReflectionTestUtils.invokeMethod(filter, "issueSuccessRedirect", request, response);

        assertEquals(302, response.getStatus());
        assertEquals("index.html", response.getRedirectedUrl());
        assertEquals(null, WebUtils.getSavedRequest(request));
    }

    @Test
    void issueSuccessRedirect_keepsSafeSavedRequest() throws Exception {
        SpmFilter filter = new SpmFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest savedReq = new MockHttpServletRequest("GET", "/index.html");
        SavedRequest saved = new SavedRequest(savedReq);
        request.setAttribute(WebUtils.SAVED_REQUEST_KEY, saved);

        ReflectionTestUtils.invokeMethod(filter, "issueSuccessRedirect", request, response);

        assertEquals(302, response.getStatus());
        assertEquals("/index.html", response.getRedirectedUrl());
    }

    private static final class SavingProbeFilter extends SpmFilter {
        boolean superSaveInvoked;

        @Override
        protected void saveRequest(ServletRequest request) {
            if (request instanceof HttpServletRequest && !WebPathUtils.shouldPersistSavedRequest((HttpServletRequest) request))
                return;
            superSaveInvoked = true;
        }
    }
}
