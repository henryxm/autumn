package cn.org.autumn.modules.sys.shiro;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LogoutSkipSupportTest {

    @Test
    void marked_detectsLogoutCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(LogoutSkipSupport.COOKIE_NAME, "1"));
        assertTrue(LogoutSkipSupport.marked(request));
    }

    @Test
    void mark_writesSkipCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogoutSkipSupport.mark(request, response);
        Cookie[] cookies = response.getCookies();
        assertTrue(cookies != null && cookies.length > 0);
        assertTrue(LogoutSkipSupport.COOKIE_NAME.equals(cookies[0].getName()));
        assertTrue(cookies[0].getMaxAge() > 0);
    }

    @Test
    void clear_writesExpiredCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogoutSkipSupport.clear(response, request);
        Cookie[] cookies = response.getCookies();
        assertTrue(cookies != null && cookies.length > 0);
        assertTrue(LogoutSkipSupport.COOKIE_NAME.equals(cookies[0].getName()));
        assertTrue(cookies[0].getMaxAge() == 0);
    }
}
