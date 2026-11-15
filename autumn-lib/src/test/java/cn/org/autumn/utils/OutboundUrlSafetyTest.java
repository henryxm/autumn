package cn.org.autumn.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OutboundUrlSafetyTest {

    @Test
    void emptyAllowlistRejectsAll() {
        assertEquals(OutboundUrlSafety.REASON_ALLOWLIST,
                OutboundUrlSafety.rejectReason("https://api.openai.com/v1", true, List.of(), false));
    }

    @Test
    void disabledRejects() {
        assertEquals(OutboundUrlSafety.REASON_DISABLED,
                OutboundUrlSafety.rejectReason("https://api.openai.com/v1", false, List.of("api.openai.com"), false));
    }

    @Test
    void allowlistedHostPassesWithoutDns() {
        assertNull(OutboundUrlSafety.rejectReason(
                "https://api.openai.com/v1/chat/completions", true, List.of("api.openai.com"), false));
        assertNull(OutboundUrlSafety.rejectReason(
                "https://sub.openai.com/x", true, List.of("*.openai.com"), false));
    }

    @Test
    void privateLiteralRejected() {
        assertEquals(OutboundUrlSafety.REASON_PRIVATE,
                OutboundUrlSafety.rejectReason("http://127.0.0.1/", true, List.of("127.0.0.1"), false));
        assertEquals(OutboundUrlSafety.REASON_PRIVATE,
                OutboundUrlSafety.rejectReason("http://169.254.169.254/latest/meta-data/", true,
                        List.of("169.254.169.254"), false));
        assertEquals(OutboundUrlSafety.REASON_PRIVATE,
                OutboundUrlSafety.rejectReason("http://10.0.0.1/", true, List.of("10.0.0.1"), false));
    }

    @Test
    void nonHttpSchemeRejected() {
        assertEquals(OutboundUrlSafety.REASON_SCHEME,
                OutboundUrlSafety.rejectReason("file:///etc/passwd", true, List.of("etc"), false));
    }

    @Test
    void webhookModeAllowsPublicWhenAllowlistEmpty() {
        assertNull(OutboundUrlSafety.rejectReasonPublicHostsOk(
                "https://hooks.example.com/cb", List.of(), false));
        assertEquals(OutboundUrlSafety.REASON_PRIVATE,
                OutboundUrlSafety.rejectReasonPublicHostsOk("http://192.168.1.1/x", List.of(), false));
    }
}
