package cn.org.autumn.modules.qrc.support;

import com.alibaba.fastjson2.JSON;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QrcSseEventStreamSupportTest {

    @Test
    public void subscribe_and_publish_withoutRedis_doNotThrow() {
        QrcSseEventStreamSupport<TestEvent> support = newSupport();
        TestEvent pending = new TestEvent("ticket-1", "PENDING");
        SseEmitter emitter = support.subscribe("ticket-1", System.currentTimeMillis() + 60_000L, pending);
        assertNotNull(emitter);
        support.publish(new TestEvent("ticket-1", "SCANNED"));
        assertTrue(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void subscribe_rejectsBlankUuid() {
        QrcSseEventStreamSupport<TestEvent> support = newSupport();
        support.subscribe("", System.currentTimeMillis() + 60_000L, new TestEvent("x", "PENDING"));
    }

    private static QrcSseEventStreamSupport<TestEvent> newSupport() {
        return new QrcSseEventStreamSupport<>(
                "TEST QRC",
                "test:qrc:sse",
                TestEvent::getUuid,
                TestEvent::getStatus,
                JSON::toJSONString,
                json -> JSON.parseObject(json, TestEvent.class));
    }

    static class TestEvent {
        private String uuid;
        private String status;

        TestEvent() {
        }

        TestEvent(String uuid, String status) {
            this.uuid = uuid;
            this.status = status;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
