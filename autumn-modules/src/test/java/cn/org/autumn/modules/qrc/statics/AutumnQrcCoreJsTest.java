package cn.org.autumn.modules.qrc.statics;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** autumn-qrc-core.js 方案 C 通知 API 契约测试。 */
public class AutumnQrcCoreJsTest {

    @Test
    public void coreJs_exposesPlanCNotifyAndUiHelpers() throws Exception {
        String js = readCoreJs();
        assertTrue(js.contains("startTicketNotify: function"));
        assertTrue(js.contains("startRpNotify: function"));
        assertTrue(js.contains("startPollFallback: function"));
        assertTrue(js.contains("resumeTicketNotify: function"));
        assertTrue(js.contains("stopNotify: function"));
        assertTrue(js.contains("wireVueScannerUi: wireVueScannerUi"));
        assertTrue(js.contains("bindPlainHostScannedUi: bindPlainHostScannedUi"));
        assertTrue(js.contains("startTicketNotify(onUnavailable)"));
    }

    @Test
    public void coreJs_doesNotUnconditionallyPollRpOnStreamSubscribe() throws Exception {
        String js = readCoreJs();
        int subscribeIdx = js.indexOf("subscribeRpStream: function");
        assertTrue("subscribeRpStream should exist", subscribeIdx >= 0);
        String subscribeBody = js.substring(subscribeIdx, Math.min(js.length(), subscribeIdx + 400));
        assertFalse("subscribeRpStream must not start parallel poll", subscribeBody.contains("setInterval"));
        assertTrue(js.contains("startPollFallback: function"));
        int fallbackIdx = js.indexOf("startPollFallback: function");
        String fallbackBody = js.substring(fallbackIdx, Math.min(js.length(), fallbackIdx + 600));
        assertTrue("fallback should poll RP status", fallbackBody.contains("pollRpStatus"));
    }

    private static String readCoreJs() throws Exception {
        InputStream in = AutumnQrcCoreJsTest.class.getResourceAsStream("/statics/js/autumn-qrc-core.js");
        assertNotNull("classpath statics/js/autumn-qrc-core.js", in);
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
