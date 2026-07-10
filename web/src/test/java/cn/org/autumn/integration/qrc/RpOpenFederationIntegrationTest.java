package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.model.AuthSiteConfig;
import cn.org.autumn.model.PageLoginSupport;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.opc.dao.ConnectBindDao;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.spi.OpenPlatformService;
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

/** 开放 OPC 扫码：跨站联邦（D）与同源（B2）集成。 */
public class RpOpenFederationIntegrationTest extends IntegrationTest {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private OpenPlatformService openPlatformService;

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private ConnectBindDao connectBindDao;

    private static final Type CREATE_RESULT = new TypeToken<Response<TicketCreateResult>>() {
    }.getType();

    private static final Type STATUS_RESULT = new TypeToken<Response<cn.org.autumn.modules.qrc.dto.TicketStatusResult>>() {
    }.getType();

    private static final Type STRING_RESULT = new TypeToken<Response<String>>() {
    }.getType();

    @Test
    void openRpFederation_createsBindAfterWebhook() throws Exception {
        String baseUrl = "http://127.0.0.1:" + port;
        String remoteBase = "http://localhost:" + port;
        String redirectUri = baseUrl + "/open/oauth2/callback";
        OpenAppRegisterOutcome registered = openPlatformService.registerApp(adminUuid, "Open IT", redirectUri, "basic", OpenAppType.Web);
        assertNotNull(registered);
        String appId = registered.getAppId();
        String secret = registered.getAppSecret();

        ConnectAppEntity app = connectAppService.saveConfig(adminUuid, appId, secret, remoteBase, redirectUri + "?appId=" + appId, "Open IT", "basic", null, null, PageLoginSupport.QR);
        assertNotNull(app);
        ClientDetailsEntity oauth = clientDetailsService.findByClientId(appId);
        assertNotNull(oauth);
        assertEquals(redirectUri + "?appId=" + appId, oauth.getRedirectUri());

        AuthSiteConfig siteConfig = sysConfigService.getAuthSiteConfig();
        siteConfig.setSiteRole(AuthSiteConfig.ROLE_AS_AND_RP);
        sysConfigService.updateValueByKey(AuthSiteConfig.CONFIG_KEY, gson.toJson(siteConfig));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> createBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("type", AuthLoginProviderType.OAUTH2_OPEN);
        data.put("id", appId);
        data.put("callback", "/welcome");
        createBody.put("data", data);

        ResponseEntity<String> createResp = restTemplate.exchange(baseUrl + "/client/oauth2/qrc/web/ticket/create", HttpMethod.POST, new HttpEntity<>(createBody, headers), String.class);
        assertEquals(200, createResp.getStatusCodeValue());
        Response<TicketCreateResult> created = gson.fromJson(createResp.getBody(), CREATE_RESULT);
        assertNotNull(created);
        assertEquals(0, created.getCode());
        assertNotNull(created.getData().getUuid());
        assertEquals(Intent.OAUTH_DEVICE, created.getData().getIntent());

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(created.getData().getUuid(), scanner);
        scanTicketService.confirm(created.getData().getUuid(), scanner, null);

        Thread.sleep(1000L);

        ConnectBindEntity bind = connectBindDao.getByConnectAppAndUser(app.getUuid(), adminUuid);
        assertNotNull(bind, "扫码确认后应建立 opc_connect_bind");
        assertNotNull(bind.getOpenId());
        assertEquals(adminUuid, bind.getUser());
    }

    @Test
    void openAsWebScan_completeViaOpcEndpoint() throws Exception {
        String baseUrl = "http://127.0.0.1:" + port;
        String redirectUri = baseUrl + "/open/oauth2/callback";
        OpenAppRegisterOutcome registered = openPlatformService.registerApp(adminUuid, "Open AS IT", redirectUri, "basic", OpenAppType.Web);
        assertNotNull(registered);
        String appId = registered.getAppId();
        String secret = registered.getAppSecret();

        ConnectAppEntity app = connectAppService.saveConfig(adminUuid, appId, secret, baseUrl, redirectUri + "?appId=" + appId, "Open AS IT", "basic", null, null, PageLoginSupport.QR);
        assertNotNull(app);
        assertNotNull(clientDetailsService.findByClientId(appId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> createBody = new HashMap<>();
        Map<String, Object> createData = new HashMap<>();
        createData.put("type", AuthLoginProviderType.OAUTH2_OPEN);
        createData.put("id", appId);
        createBody.put("data", createData);

        ResponseEntity<String> createResp = restTemplate.exchange(baseUrl + "/qrc/scanticket/web/ticket/create", HttpMethod.POST, new HttpEntity<>(createBody, headers), String.class);
        assertEquals(200, createResp.getStatusCodeValue());
        Response<TicketCreateResult> created = gson.fromJson(createResp.getBody(), CREATE_RESULT);
        assertNotNull(created);
        assertEquals(0, created.getCode());
        assertEquals(Intent.OAUTH_DEVICE, created.getData().getIntent());

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(created.getData().getUuid(), scanner);
        scanTicketService.confirm(created.getData().getUuid(), scanner, null);

        ResponseEntity<String> statusResp = restTemplate.getForEntity(baseUrl + "/qrc/scanticket/web/ticket/status?uuid=" + created.getData().getUuid(), String.class);
        Response<cn.org.autumn.modules.qrc.dto.TicketStatusResult> status = gson.fromJson(statusResp.getBody(), STATUS_RESULT);
        assertNotNull(status);
        assertEquals(0, status.getCode());
        assertEquals("COMPLETED", status.getData().getStatus());
        assertNotNull(status.getData().getResult().get("code"));

        Map<String, Object> completeBody = new HashMap<>();
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("type", AuthLoginProviderType.OAUTH2_OPEN);
        completeData.put("id", appId);
        completeData.put("code", status.getData().getResult().get("code"));
        completeBody.put("data", completeData);

        ResponseEntity<String> completeResp = restTemplate.exchange(baseUrl + "/open/oauth2/qrc/web/complete", HttpMethod.POST, new HttpEntity<>(completeBody, headers), String.class);
        Response<String> completed = gson.fromJson(completeResp.getBody(), STRING_RESULT);
        assertNotNull(completed);
        assertEquals(0, completed.getCode());
        assertTrue(completed.getData() != null && !completed.getData().isEmpty());

        ConnectBindEntity bind = connectBindDao.getByConnectAppAndUser(app.getUuid(), adminUuid);
        assertNotNull(bind);
        assertNotNull(bind.getOpenId());
    }
}
