package cn.org.autumn.modules.qrc.support;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanBrowserBindSupportTest {

    private ScanBrowserBindSupport support;
    private ScanTicketService scanTicketService;

    @BeforeEach
    void setUp() {
        support = new ScanBrowserBindSupport();
        scanTicketService = mock(ScanTicketService.class);
        ReflectionTestUtils.setField(support, "scanTicketService", scanTicketService);
    }

    @Test
    void bindAndAssert_ok() {
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid("t-1");
        ticket.setPayload(new HashMap<>());
        MockHttpServletResponse response = new MockHttpServletResponse();
        support.bindOnCreate(ticket, response);
        verify(scanTicketService).persistTicket(ticket);
        verify(scanTicketService).putBrowserBind(eq("t-1"), anyString(), anyLong());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookies());
        assertDoesNotThrow(() -> support.assertBound(ticket, request));
        assertNotNull(response.getCookie(ScanBrowserBindSupport.cookieNameForUuid("t-1")));
    }

    @Test
    void multiTab_cookiesDoNotOverwrite() {
        TicketSnapshot a = new TicketSnapshot();
        a.setUuid("tab-a");
        a.setPayload(new HashMap<>());
        TicketSnapshot b = new TicketSnapshot();
        b.setUuid("tab-b");
        b.setPayload(new HashMap<>());
        MockHttpServletResponse response = new MockHttpServletResponse();
        support.bindOnCreate(a, response);
        support.bindOnCreate(b, response);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookies());
        assertDoesNotThrow(() -> support.assertBound(a, request));
        assertDoesNotThrow(() -> support.assertBound(b, request));
        assertTrue(response.getCookies().length >= 4);
    }

    @Test
    void assertBound_rejectsOtherBrowser() {
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid("t-2");
        ticket.setPayload(new HashMap<>());
        MockHttpServletResponse response = new MockHttpServletResponse();
        support.bindOnCreate(ticket, response);

        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(AException.class, () -> support.assertBound(ticket, request));
    }

    @Test
    void bindByUuid_redisOrLocalFallback() {
        when(scanTicketService.findTicket("rp-1")).thenReturn(null);
        final String[] held = {null};
        org.mockito.Mockito.doAnswer(inv -> {
            held[0] = inv.getArgument(1);
            return null;
        }).when(scanTicketService).putBrowserBind(eq("rp-1"), anyString(), anyLong());
        when(scanTicketService.getBrowserBind("rp-1")).thenAnswer(inv -> held[0]);

        MockHttpServletResponse response = new MockHttpServletResponse();
        support.bindOnCreate("rp-1", response);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookies());
        assertDoesNotThrow(() -> support.assertBoundByUuid("rp-1", request));
    }

    @Test
    void legacyTicketWithoutBind_allowed() {
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid("legacy-1");
        ticket.setPayload(new HashMap<>()); // 无 browserBind 键
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertDoesNotThrow(() -> support.assertBound(ticket, request));
    }

    @Test
    void legacySingleCookie_stillAccepted() {
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid("leg-cookie");
        ticket.setPayload(new HashMap<>());
        ticket.getPayload().put(ScanBrowserBindSupport.PAYLOAD_KEY, "sec-xyz");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ScanBrowserBindSupport.COOKIE_NAME, "leg-cookie.sec-xyz"));
        assertDoesNotThrow(() -> support.assertBound(ticket, request));
    }
}
