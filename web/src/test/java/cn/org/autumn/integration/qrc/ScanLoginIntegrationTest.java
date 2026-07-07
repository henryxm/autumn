package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.model.TicketStatus;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.qrc.shiro.ScanLoginToken;
import cn.org.autumn.database.CrudGuard;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ScanLoginIntegrationTest extends IntegrationTest {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    private static final Type CREATE_RESULT = new TypeToken<Response<TicketCreateResult>>() {
    }.getType();

    private static final Type STATUS_RESULT = new TypeToken<Response<TicketStatusResult>>() {
    }.getType();

    @Test
    void selfWebLoginScannedStatusShowsScannerBrief() throws Exception {
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.SELF_WEB_LOGIN);
        ctx.setIp("127.0.0.1");
        TicketSnapshot ticket = scanTicketService.create(ctx);
        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(ticket.getUuid(), scanner);

        ResponseEntity<String> response = restTemplate.getForEntity("http://127.0.0.1:" + port + "/qrc/scanticket/web/ticket/status?uuid=" + ticket.getUuid(), String.class);
        assertEquals(200, response.getStatusCodeValue());
        Response<TicketStatusResult> parsed = gson.fromJson(response.getBody(), STATUS_RESULT);
        assertNotNull(parsed);
        assertEquals(0, parsed.getCode());
        assertEquals(TicketStatus.SCANNED, parsed.getData().getStatus());
        assertNotNull(parsed.getData().getScannerBrief());
        assertNotNull(parsed.getData().getScannerBrief().getDisplayName());
        assertNull(parsed.getData().getExchange());

        scanTicketService.confirm(ticket.getUuid(), scanner, null);
        TicketSnapshot confirmed = scanTicketService.getRequired(ticket.getUuid());
        assertNotNull(confirmed.getExchange());
    }

    @Test
    void selfWebLoginFlow() throws Exception {
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.SELF_WEB_LOGIN);
        ctx.setIp("127.0.0.1");
        TicketSnapshot ticket = scanTicketService.create(ctx);
        assertNotNull(ticket.getUuid());

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(ticket.getUuid(), scanner);
        scanTicketService.confirm(ticket.getUuid(), scanner, null);

        TicketSnapshot updated = scanTicketService.getRequired(ticket.getUuid());
        assertNotNull(updated.getExchange());

        CrudGuard.force(() -> sysUserService.login(new ScanLoginToken(updated.getExchange())));
        assertTrue(ShiroUtils.isLogin());
    }

    @Test
    void oauthAuthorizeFlow() throws Exception {
        String clientId = "qrc-it-oauth-" + System.currentTimeMillis();
        String baseUrl = "http://127.0.0.1:" + port;
        String redirectUri = baseUrl + "/client/oauth2/callback";
        ClientDetailsEntity client = clientDetailsService.create(baseUrl, clientId, "it-secret", null, "IT OAuth", "QRC integration");
        assertNotNull(client);

        Map<String, String> payload = new HashMap<>();
        payload.put("clientId", clientId);
        payload.put("redirectUri", redirectUri);
        payload.put("scope", "basic");
        payload.put("state", "it-state");
        payload.put("callback", "");
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.OAUTH_AUTHORIZE);
        ctx.setClientId(clientId);
        ctx.setPayload(payload);
        ctx.setIp("127.0.0.1");

        TicketSnapshot ticket = scanTicketService.create(ctx);
        assertNotNull(ticket.getUuid());

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(ticket.getUuid(), scanner);
        ConfirmResult result = scanTicketService.confirm(ticket.getUuid(), scanner, null);

        assertNotNull(result.getExchange());
        assertFalse(result.isCompleted());
        TicketSnapshot updated = scanTicketService.getRequired(ticket.getUuid());
        assertEquals(TicketStatus.CONFIRMED, updated.getStatus());

        CrudGuard.force(() -> sysUserService.login(new ScanLoginToken(result.getExchange())));
        assertTrue(ShiroUtils.isLogin());
    }

    @Test
    void webCreateTicketApi() {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("intent", Intent.SELF_WEB_LOGIN);
        body.put("data", data);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange("http://127.0.0.1:" + port + "/qrc/scanticket/web/ticket/create", HttpMethod.POST, entity, String.class);
        assertEquals(200, response.getStatusCodeValue());
        Response<TicketCreateResult> parsed = gson.fromJson(response.getBody(), CREATE_RESULT);
        assertNotNull(parsed);
        assertEquals(0, parsed.getCode());
        assertNotNull(parsed.getData().getUuid());
        assertNotNull(parsed.getData().getQrUrl());
    }

    @Test
    void oauthDeviceOpenCreateFlow() throws Exception {
        String clientId = "qrc-it-device-" + System.currentTimeMillis();
        String baseUrl = "http://127.0.0.1:" + port;
        String redirectUri = baseUrl + "/client/oauth2/callback";
        ClientDetailsEntity client = clientDetailsService.create(baseUrl, clientId, "it-secret", null, "IT Device", "QRC device integration");
        assertNotNull(client);

        Map<String, Object> openBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        data.put("clientSecret", "it-secret");
        data.put("redirectUri", redirectUri);
        data.put("scope", "basic");
        data.put("state", "device-state");
        openBody.put("data", data);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> createResp = restTemplate.exchange(baseUrl + "/qrc/api/v1/ticket/open/create", HttpMethod.POST, new HttpEntity<>(openBody, headers), String.class);
        assertEquals(200, createResp.getStatusCodeValue());
        Response<TicketCreateResult> created = gson.fromJson(createResp.getBody(), CREATE_RESULT);
        assertNotNull(created);
        assertEquals(0, created.getCode());
        assertNotNull(created.getData().getUuid());
        assertEquals(Intent.OAUTH_DEVICE, created.getData().getIntent());

        String uuid = created.getData().getUuid();
        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(uuid, scanner);
        scanTicketService.confirm(uuid, scanner, null);

        Map<String, Object> statusBody = new HashMap<>();
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("uuid", uuid);
        statusData.put("clientId", clientId);
        statusData.put("clientSecret", "it-secret");
        statusBody.put("data", statusData);
        ResponseEntity<String> statusResp = restTemplate.exchange(baseUrl + "/qrc/api/v1/ticket/open/status", HttpMethod.POST, new HttpEntity<>(statusBody, headers), String.class);
        assertEquals(200, statusResp.getStatusCodeValue());
        Response<TicketStatusResult> status = gson.fromJson(statusResp.getBody(), STATUS_RESULT);
        assertNotNull(status);
        assertEquals(0, status.getCode());
        assertEquals(TicketStatus.COMPLETED, status.getData().getStatus());
        assertNotNull(status.getData().getResult().get("code"));
        assertTrue(clientDetailsService.isValidCode(status.getData().getResult().get("code")));
    }

    @Test
    void denyTicket() throws Exception {
        CreateContext ctx = new CreateContext();
        ctx.setIntent(Intent.SELF_WEB_LOGIN);
        TicketSnapshot ticket = scanTicketService.create(ctx);
        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(ticket.getUuid(), scanner);
        TicketSnapshot denied = scanTicketService.deny(ticket.getUuid(), scanner);
        assertEquals(TicketStatus.DENIED, denied.getStatus());
    }
}
