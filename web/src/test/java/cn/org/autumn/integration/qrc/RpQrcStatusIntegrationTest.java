package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.client.service.RpQrcPendingStore;
import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** RP 联邦 ticket/status 降级轮询：SCANNED 后返回 scannerBrief。 */
public class RpQrcStatusIntegrationTest extends IntegrationTest {

    @Autowired
    private RpQrcPendingStore rpQrcPendingStore;

    @Test
    void ticketStatus_afterScanned_returnsScannerBrief() {
        String uuid = "rp-status-" + System.currentTimeMillis();
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid(uuid);
        pending.setStatus("SCANNED");
        pending.setCredentialType("oauth2_classic");
        pending.setCredentialId("integration-demo");
        ScannerBrief brief = new ScannerBrief();
        brief.setDisplayName("Status Tester");
        brief.setIcon("/statics/img/auth-login-default.svg");
        pending.setScannerBrief(brief);
        pending.setExpiredAt(System.currentTimeMillis() + 120_000L);
        rpQrcPendingStore.save(pending);

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + "/client/oauth2/qrc/web/ticket/status?uuid=" + uuid,
                String.class);
        assertEquals(200, resp.getStatusCodeValue());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertNotNull(body);
        assertEquals(0, body.getIntValue("code"));
        JSONObject data = body.getJSONObject("data");
        assertNotNull(data);
        assertEquals("SCANNED", data.getString("status"));
        JSONObject scannerBrief = data.getJSONObject("scannerBrief");
        assertNotNull(scannerBrief);
        assertEquals("Status Tester", scannerBrief.getString("displayName"));
    }
}
