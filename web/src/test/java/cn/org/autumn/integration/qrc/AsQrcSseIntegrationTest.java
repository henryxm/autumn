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
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** B2 同源 SSE：scan 后 PendingStore 更新，status 降级端点可返回 scannerBrief。 */
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
}
