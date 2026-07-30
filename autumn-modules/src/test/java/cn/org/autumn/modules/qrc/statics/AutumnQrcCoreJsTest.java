package cn.org.autumn.modules.qrc.statics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** autumn-qrc-core.js 方案 C 通知 API 契约测试。 */
class AutumnQrcCoreJsTest {

    @Test
    void coreJs_exposesPlanCNotifyAndUiHelpers() throws Exception {
        String js = readCoreJs();
        assertTrue(js.contains("startTicketNotify: function"));
        assertTrue(js.contains("startSseNotify: function"));
        assertTrue(js.contains("startRpNotify: function"));
        assertTrue(js.contains("startPollFallback: function"));
        assertTrue(js.contains("resumeTicketNotify: function"));
        assertTrue(js.contains("stopNotify: function"));
        assertTrue(js.contains("handleStreamEvent: function"));
        assertTrue(js.contains("wireVueScannerUi: wireVueScannerUi"));
        assertTrue(js.contains("bindPlainHostScannedUi: bindPlainHostScannedUi"));
        assertTrue(js.contains("startTicketNotify(onUnavailable)"));
        assertTrue(js.contains("markSseHealthy: function"));
        assertTrue(js.contains("data.status === 'PENDING'"));
        assertTrue(js.contains("markQrScanned: function"));
        assertTrue(js.contains("completeQrRedirect: function"));
        assertTrue(js.contains("recoverAfterMissingSession: function"));
        assertTrue(js.contains("exchange: data.exchange, rememberMe: true")
                || js.contains("rememberMe: true, exchange: data.exchange"),
                "exchange should default rememberMe true");
    }

    @Test
    void coreJs_doesNotUnconditionallyPollRpOnStreamSubscribe() throws Exception {
        String js = readCoreJs();
        int subscribeIdx = js.indexOf("subscribeRpStream: function");
        assertTrue(subscribeIdx >= 0, "subscribeRpStream should exist");
        String subscribeBody = js.substring(subscribeIdx, Math.min(js.length(), subscribeIdx + 400));
        assertFalse(subscribeBody.contains("setInterval"), "subscribeRpStream must not start parallel poll");
        assertTrue(js.contains("startPollFallback: function"));
        int fallbackIdx = js.indexOf("startPollFallback: function");
        String fallbackBody = js.substring(fallbackIdx, Math.min(js.length(), fallbackIdx + 900));
        assertTrue(fallbackBody.contains("pollQrStatus"), "fallback should poll by mode");
        assertFalse(js.contains("this.startAsPoll(onUnavailable)"), "startTicketNotify must not call startAsPoll directly");
        assertFalse(js.contains("self.startAsPoll(resumeOpts"), "resumeTicketNotify must not call startAsPoll directly");
    }

    private static String readCoreJs() throws Exception {
        InputStream in = AutumnQrcCoreJsTest.class.getResourceAsStream("/statics/js/autumn-qrc-core.js");
        assertNotNull(in, "classpath statics/js/autumn-qrc-core.js");
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
