package cn.org.autumn.integration.oauth;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.qrc.dto.SessionExchangeRequest;
import cn.org.autumn.modules.qrc.support.ScanWebSupport;
import cn.org.autumn.modules.oauth.oauth2.support.AuthAuthorizeLoginSupport;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OAuth 授权页 loginTab 记忆：账号登录 redirect 与扫码 exchange 后会话态下的授权页配置。
 */
@ActiveProfiles({"it", "h2"})
public class OAuthAuthorizeTabIntegrationTest extends IntegrationTest {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private ScanWebSupport scanWebSupport;

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void accountLoginRedirectIncludesLoginTab() throws Exception {
        String clientId = "oauth-tab-acct-" + System.currentTimeMillis();
        String redirectUri = baseUrl() + "/client/oauth2/callback";
        ClientDetailsEntity client = clientDetailsService.create(baseUrl(), clientId, "it-secret", null, "Tab IT", "loginTab test");
        assertNotNull(client);

        String authorizeUrl = baseUrl() + "/oauth2/authorize?response_type=code"
                + "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                + "&state=tab-it";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", sysUserService.getAdmin());
        form.add("password", "admin");
        form.add("callback", authorizeUrl);
        form.add("loginTab", "account");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        RestTemplate noRedirect = noRedirectRestTemplate();
        ResponseEntity<String> response = noRedirect.exchange(
                baseUrl() + "/oauth2/login",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class);

        assertTrue(response.getStatusCode().is3xxRedirection(), "expected redirect after oauth login");
        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().contains("loginTab=account"),
                "redirect should preserve login tab, got: " + location);
    }

    @Test
    void qrExchangeSavesLoginTabInSession() throws Exception {
        String clientId = "oauth-tab-qr-" + System.currentTimeMillis();
        String redirectUri = baseUrl() + "/client/oauth2/callback";
        ClientDetailsEntity client = clientDetailsService.create(baseUrl(), clientId, "it-secret", null, "Tab QR IT", "loginTab qr");
        assertNotNull(client);

        Map<String, String> payload = new HashMap<>();
        payload.put("clientId", clientId);
        payload.put("redirectUri", redirectUri);
        payload.put("scope", "basic");
        payload.put("state", "qr-tab");
        payload.put("callback", "");

        cn.org.autumn.modules.qrc.dto.CreateContext ctx = new cn.org.autumn.modules.qrc.dto.CreateContext();
        ctx.setIntent(Intent.OAUTH_AUTHORIZE);
        ctx.setClientId(clientId);
        ctx.setPayload(payload);
        ctx.setIp("127.0.0.1");

        TicketSnapshot ticket = scanTicketService.create(ctx);
        SysUserEntity scanner = sysUserService.getByUuid(adminUuid);
        scanTicketService.scan(ticket.getUuid(), scanner);
        ConfirmResult result = scanTicketService.confirm(ticket.getUuid(), scanner, null);
        assertNotNull(result.getExchange());

        MockHttpServletRequest servlet = new MockHttpServletRequest();
        servlet.setSession(new MockHttpSession(null));
        SessionExchangeRequest exchangeRequest = new SessionExchangeRequest();
        exchangeRequest.setExchange(result.getExchange());
        String redirect = scanWebSupport.exchangeSession(exchangeRequest, servlet);

        assertTrue(redirect.contains("loginTab=qr"), "redirect should carry qr tab: " + redirect);
        assertEquals("qr", AuthAuthorizeLoginSupport.resolveLoginTab(servlet));
    }

    private static RestTemplate noRedirectRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        return new RestTemplateBuilder().requestFactory(() -> factory).build();
    }
}
