package cn.org.autumn.modules.sys.shiro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.org.autumn.modules.sys.service.SysConfigService;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class HostSessionCookieFilterTest {

    @AfterEach
    void tearDown() {
        HostSessionCookieContext.clear();
    }

    @Test
    void doFilter_setsAndClearsThreadLocalContext() throws Exception {
        SysConfigService sysConfigService = mock(SysConfigService.class);
        when(sysConfigService.getSiteDomainList()).thenReturn(Arrays.asList("a.com", "b.com"));
        when(sysConfigService.getClusterRootDomain()).thenReturn("");

        HostSessionCookieFilter filter = new HostSessionCookieFilter();
        ReflectionTestUtils.setField(filter, "sysConfigService", sysConfigService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.addHeader("Host", "b.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            HostSessionCookieHolder holder = HostSessionCookieContext.get();
            assertEquals(HostSessionCookieSupport.DEFAULT_COOKIE_NAME, holder.getName());
            assertNull(holder.getDomain());
        });

        assertNull(HostSessionCookieContext.get());
    }

    @Test
    void hostAwareCookie_readsContextPerRequest() {
        HostAwareSessionIdCookie cookie = new HostAwareSessionIdCookie("autumnid");
        cookie.setName("fallback");
        cookie.setDomain("fallback.example.com");

        HostSessionCookieContext.set(new HostSessionCookieHolder("b.com", null));
        assertEquals("b.com", cookie.getName());
        assertNull(cookie.getDomain());

        HostSessionCookieContext.clear();
        assertEquals("fallback", cookie.getName());
        assertEquals("fallback.example.com", cookie.getDomain());
    }
}
