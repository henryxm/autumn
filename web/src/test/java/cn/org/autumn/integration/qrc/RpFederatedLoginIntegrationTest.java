package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.model.AuthSiteConfig;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.client.dao.WebOauthBindDao;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketStatus;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RpFederatedLoginIntegrationTest extends IntegrationTest {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private WebOauthBindDao webOauthBindDao;

    private static final Type CREATE_RESULT = new TypeToken<Response<TicketCreateResult>>() {
    }.getType();

    private static final Type STATUS_RESULT = new TypeToken<Response<TicketStatusResult>>() {
    }.getType();

    @Test
    void rpQrcWebComplete_bindChoiceThenCreate() throws Exception {
        String clientId = "qrc-it-rp-" + System.currentTimeMillis();
        String baseUrl = "http://127.0.0.1:" + port;
        String redirectUri = baseUrl + "/client/oauth2/callback";
        ClientDetailsEntity oauth = clientDetailsService.create(baseUrl, clientId, "rp-secret", null, "RP IT", "RP integration");
        assertNotNull(oauth);

        WebAuthenticationEntity web = webAuthenticationService.create(baseUrl, clientId, "rp-secret", null, "RP IT", "basic", "normal");
        assertNotNull(web);
        web.setOriginUri(baseUrl);
        web.setRedirectUri(redirectUri);
        webAuthenticationService.updateById(web);

        AuthSiteConfig siteConfig = sysConfigService.getAuthSiteConfig();
        siteConfig.setSiteRole(AuthSiteConfig.ROLE_AS_AND_RP);
        sysConfigService.updateValueByKey(AuthSiteConfig.CONFIG_KEY, gson.toJson(siteConfig));
        sysConfigService.updateValueByKey(SysConfigService.LOGIN_AUTHENTICATION, "oauth2:" + clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> createResp = restTemplate.exchange(baseUrl + "/client/oauth2/qrc/web/ticket/create", HttpMethod.POST, new HttpEntity<>(new HashMap<>(), headers), String.class);
        assertEquals(200, createResp.getStatusCodeValue());
        Response<TicketCreateResult> created = gson.fromJson(createResp.getBody(), CREATE_RESULT);
        assertNotNull(created);
        assertEquals(0, created.getCode());
        assertNotNull(created.getData().getUuid());
        assertEquals(Intent.OAUTH_DEVICE, created.getData().getIntent());

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(created.getData().getUuid(), scanner);
        scanTicketService.confirm(created.getData().getUuid(), scanner, null);

        ResponseEntity<String> statusResp = restTemplate.exchange(baseUrl + "/client/oauth2/qrc/web/ticket/status?uuid=" + created.getData().getUuid(), HttpMethod.POST, new HttpEntity<>(headers), String.class);
        assertEquals(200, statusResp.getStatusCodeValue());
        Response<TicketStatusResult> status = gson.fromJson(statusResp.getBody(), STATUS_RESULT);
        assertNotNull(status);
        assertEquals(0, status.getCode());
        assertEquals(TicketStatus.COMPLETED, status.getData().getStatus());
        assertNotNull(status.getData().getResult().get("code"));

        Map<String, Object> completeBody = new HashMap<>();
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("uuid", created.getData().getUuid());
        completeBody.put("data", completeData);
        ResponseEntity<String> completeResp = restTemplate.exchange(baseUrl + "/client/oauth2/qrc/web/ticket/complete", HttpMethod.POST, new HttpEntity<>(completeBody, headers), String.class);
        assertEquals(200, completeResp.getStatusCodeValue(), completeResp.getBody());
        JsonObject completeR = JsonParser.parseString(completeResp.getBody()).getAsJsonObject();
        assertNotNull(completeR, completeResp.getBody());
        assertEquals(0, completeR.get("code").getAsInt(), completeResp.getBody());
        String choiceUrl = completeR.get("data").getAsString();
        assertNotNull(choiceUrl);
        assertTrue(choiceUrl.contains("/client/oauth2/bind/choice"), choiceUrl);
        String token = choiceUrl.substring(choiceUrl.indexOf("token=") + 6);
        int amp = token.indexOf('&');
        if (amp >= 0) {
            token = token.substring(0, amp);
        }

        HttpHeaders formHeaders = new HttpHeaders();
        formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.util.LinkedMultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
        form.add("token", token);
        ResponseEntity<String> bindCreateResp = restTemplate.exchange(baseUrl + "/client/oauth2/bind/create", HttpMethod.POST, new HttpEntity<>(form, formHeaders), String.class);
        assertTrue(bindCreateResp.getStatusCode().is2xxSuccessful() || bindCreateResp.getStatusCode().is3xxRedirection(), bindCreateResp.getBody());

        WebOauthBindEntity bind = webOauthBindDao.getByAuthenticationAndUpper(web.getUuid(), adminUuid);
        assertNotNull(bind);
        assertNotEquals(adminUuid, bind.getUser());
        SysUserEntity localUser = sysUserService.getByUuid(bind.getUser());
        assertNotNull(localUser);
        assertTrue(localUser.getStatus() >= 1);
    }
}
