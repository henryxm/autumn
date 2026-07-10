package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketStatus;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 标准二（B3）：第三方服务端建票 + 轮询，无需网页 UI。
 */
public class OpenApiServerScanLoginIntegrationTest extends IntegrationTest {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    private static final Type CREATE_RESULT = new TypeToken<Response<TicketCreateResult>>() {
    }.getType();

    private static final Type STATUS_RESULT = new TypeToken<Response<TicketStatusResult>>() {
    }.getType();

    @Test
    void serverSideOpenApi_createPollAndReceiveCode() throws Exception {
        String clientId = "qrc-it-server-" + System.currentTimeMillis();
        String baseUrl = "http://127.0.0.1:" + port;
        String redirectUri = baseUrl + "/client/oauth2/callback";
        ClientDetailsEntity client = clientDetailsService.create(baseUrl, clientId, "server-secret", null, "Server IT", "B3 server scan");
        assertNotNull(client);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> openBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        data.put("clientSecret", "server-secret");
        data.put("redirectUri", redirectUri);
        data.put("scope", "basic");
        data.put("state", "server-state");
        openBody.put("data", data);

        ResponseEntity<String> createResp = restTemplate.exchange(baseUrl + "/qrc/api/v1/ticket/open/create", HttpMethod.POST, new HttpEntity<>(openBody, headers), String.class);
        assertEquals(200, createResp.getStatusCodeValue());
        Response<TicketCreateResult> created = gson.fromJson(createResp.getBody(), CREATE_RESULT);
        assertNotNull(created);
        assertEquals(0, created.getCode());
        assertNotNull(created.getData().getUuid());
        assertNotNull(created.getData().getQrUrl());
        assertEquals(Intent.OAUTH_DEVICE, created.getData().getIntent());

        String uuid = created.getData().getUuid();
        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(uuid, scanner);
        scanTicketService.confirm(uuid, scanner, null);

        Map<String, Object> statusBody = new HashMap<>();
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("uuid", uuid);
        statusData.put("clientId", clientId);
        statusData.put("clientSecret", "server-secret");
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
    void serverSideOpenApi_cancelTicket() throws Exception {
        String clientId = "qrc-it-cancel-" + System.currentTimeMillis();
        String baseUrl = "http://127.0.0.1:" + port;
        clientDetailsService.create(baseUrl, clientId, "cancel-secret", null, "Cancel IT", "B3 cancel");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> openBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        data.put("clientSecret", "cancel-secret");
        openBody.put("data", data);

        ResponseEntity<String> createResp = restTemplate.exchange(baseUrl + "/qrc/api/v1/ticket/open/create", HttpMethod.POST, new HttpEntity<>(openBody, headers), String.class);
        Response<TicketCreateResult> created = gson.fromJson(createResp.getBody(), CREATE_RESULT);
        String uuid = created.getData().getUuid();

        Map<String, Object> cancelBody = new HashMap<>();
        Map<String, Object> cancelData = new HashMap<>();
        cancelData.put("uuid", uuid);
        cancelData.put("clientId", clientId);
        cancelData.put("clientSecret", "cancel-secret");
        cancelBody.put("data", cancelData);

        ResponseEntity<String> cancelResp = restTemplate.exchange(baseUrl + "/qrc/api/v1/ticket/open/cancel", HttpMethod.POST, new HttpEntity<>(cancelBody, headers), String.class);
        assertEquals(200, cancelResp.getStatusCodeValue());
        Response<TicketStatusResult> cancelled = gson.fromJson(cancelResp.getBody(), STATUS_RESULT);
        assertEquals(0, cancelled.getCode());
        assertEquals(TicketStatus.CANCELLED, cancelled.getData().getStatus());
    }
}
