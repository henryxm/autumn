package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.model.AsQrcStreamEvent;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.AsQrcEventStreamService;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B2 同源 SSE：subscribe + scan 推送；stream/status HTTP 端点可用。 */
public class AsQrcSseIntegrationTest extends IntegrationTest {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private AsQrcEventStreamService asQrcEventStreamService;

    @Test
    void subscribe_catchUp_and_scan_updatesState() throws Exception {
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.SELF_WEB_LOGIN);
        ctx.setIp("127.0.0.1");
        TicketSnapshot ticket = scanTicketService.create(ctx);

        SseEmitter emitter = asQrcEventStreamService.subscribe(
                ticket.getUuid(),
                ticket.getExpired(),
                AsQrcStreamEvent.from(scanTicketService.toStatusResult(ticket)));
        assertNotNull(emitter);

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(ticket.getUuid(), scanner);

        TicketSnapshot stored = scanTicketService.getRequired(ticket.getUuid());
        assertEquals("SCANNED", stored.getStatus());

        ResponseEntity<String> statusResp = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + "/qrc/scanticket/web/ticket/status?uuid=" + ticket.getUuid(),
                String.class);
        assertEquals(200, statusResp.getStatusCodeValue());
        JSONObject body = JSON.parseObject(statusResp.getBody());
        assertEquals(0, body.getIntValue("code"));
        JSONObject data = body.getJSONObject("data");
        assertEquals("SCANNED", data.getString("status"));
        assertNotNull(data.getJSONObject("scannerBrief"));
        emitter.complete();
    }

    @Test
    void httpStreamEndpoint_acceptsSubscription() {
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.SELF_WEB_LOGIN);
        ctx.setIp("127.0.0.1");
        TicketSnapshot ticket;
        try {
            ticket = scanTicketService.create(ctx);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "text/event-stream");
        ResponseEntity<String> streamResp = restTemplate.exchange(
                "http://127.0.0.1:" + port + "/qrc/scanticket/web/ticket/stream?uuid=" + ticket.getUuid(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertTrue(streamResp.getStatusCode().is2xxSuccessful() || streamResp.getStatusCodeValue() == 200);
    }
}
