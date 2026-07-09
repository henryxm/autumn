package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.model.AuthSiteConfig;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.ScanLoginFacade;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.OpenTicketStatusRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketStatus;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ScanLoginFacade 进程内调用与 HTTP 契约一致性。
 */
public class ScanLoginFacadeIntegrationTest extends IntegrationTest {

    @Autowired
    private ScanLoginFacade scanLoginFacade;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private SysConfigService sysConfigService;

    @Test
    void facade_createAsWebTicket_matchesService() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TicketCreateRequest data = new TicketCreateRequest();
        data.setIntent(Intent.SELF_WEB_LOGIN);
        TicketCreateResult result = scanLoginFacade.createAsWebTicket(request, data);
        assertNotNull(result.getUuid());
        assertNotNull(result.getQrUrl());
        TicketStatusResult status = scanLoginFacade.pollAsWebStatus(result.getUuid());
        assertEquals(TicketStatus.PENDING, status.getStatus());
    }

    @Test
    void facade_createOpenTicket_matchesOpenApi() throws Exception {
        String clientId = "qrc-facade-open-" + System.currentTimeMillis();
        String baseUrl = "http://127.0.0.1:" + port;
        clientDetailsService.create(baseUrl, clientId, "facade-secret", null, "Facade", "facade open");

        OpenTicketCreateRequest open = new OpenTicketCreateRequest();
        open.setClientId(clientId);
        open.setClientSecret("facade-secret");
        open.setRedirectUri(baseUrl + "/client/oauth2/callback");
        open.setScope("basic");

        MockHttpServletRequest request = new MockHttpServletRequest();
        TicketCreateResult created = scanLoginFacade.createOpenTicket(open, request);
        assertNotNull(created.getUuid());
        assertEquals(Intent.OAUTH_DEVICE, created.getIntent());

        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(created.getUuid(), scanner);
        scanTicketService.confirm(created.getUuid(), scanner, null);

        OpenTicketStatusRequest statusReq = new OpenTicketStatusRequest();
        statusReq.setUuid(created.getUuid());
        statusReq.setClientId(clientId);
        statusReq.setClientSecret("facade-secret");
        TicketStatusResult status = scanLoginFacade.pollOpenTicket(statusReq);
        assertEquals(TicketStatus.COMPLETED, status.getStatus());
        assertNotNull(status.getResult().get("code"));
    }

    @Test
    void facade_createRpTicket_qrUrlUsesOriginUriHost() throws Exception {
        String clientId = "qrc-facade-rp-" + System.currentTimeMillis();
        String localBase = "http://127.0.0.1:" + port;
        String redirectUri = localBase + "/client/oauth2/callback";

        ClientDetailsEntity oauth = clientDetailsService.create(localBase, clientId, "rp-facade-secret", null, "RP Facade", "federation");
        assertNotNull(oauth);

        WebAuthenticationEntity web = webAuthenticationService.create(localBase, clientId, "rp-facade-secret", null, "RP Facade", "basic", "normal");
        assertNotNull(web);
        web.setOriginUri(localBase);
        web.setRedirectUri(redirectUri);
        webAuthenticationService.updateById(web);

        AuthSiteConfig siteConfig = sysConfigService.getAuthSiteConfig();
        siteConfig.setSiteRole(AuthSiteConfig.ROLE_AS_AND_RP);
        siteConfig.setQrcWebMode("rp");
        sysConfigService.updateValueByKey(AuthSiteConfig.CONFIG_KEY, gson.toJson(siteConfig));
        sysConfigService.updateValueByKey(SysConfigService.LOGIN_AUTHENTICATION, "oauth2:" + clientId);

        MockHttpServletRequest request = new MockHttpServletRequest();

        TicketCreateResult created = scanLoginFacade.createRpTicket(request, "/");
        assertNotNull(created.getUuid());
        assertNotNull(created.getQrUrl());
        assertTrue(created.getQrUrl().contains("/qrc/api/v1/t/" + created.getUuid()), "qrUrl 应为 AS 侧票据链接，而非 RP 本域遗留地址");
        assertEquals(Intent.OAUTH_DEVICE, created.getIntent());
    }
}
