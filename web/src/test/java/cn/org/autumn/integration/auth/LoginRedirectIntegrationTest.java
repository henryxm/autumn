package cn.org.autumn.integration.auth;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import com.alibaba.fastjson.JSONObject;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录后跳转回归：XHR/API 401 不应污染 SavedRequest，登录后不得整页打开 REST 端点。
 */
@ActiveProfiles({"it", "h2"})
public class LoginRedirectIntegrationTest extends IntegrationTest {

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void ajaxLogin_afterJsonUnauthorized_doesNotRedirectToRestApi() {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        ResponseEntity<String> unauthorized = restTemplate.exchange(
                baseUrl() + "/sys/menu/nav",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders),
                String.class);
        assertEquals(401, unauthorized.getStatusCodeValue());

        JSONObject login = postLogin();
        IntegrationJson.assertSuccess(login);
        String redirect = login.getString("data");
        assertNotNull(redirect);
        assertFalse(looksLikeRestApiRedirect(redirect));
        assertTrue(looksLikeSafeHomeRedirect(redirect));
    }

    @Test
    void ajaxLogin_afterBrowsingIndexHtml_canRedirectBackToIndex() {
        restTemplate.getForEntity(baseUrl() + "/index.html", String.class);

        JSONObject login = postLogin();
        IntegrationJson.assertSuccess(login);
        String redirect = login.getString("data");
        assertNotNull(redirect);
        assertFalse(looksLikeRestApiRedirect(redirect));
        assertTrue(redirect.contains("index.html") || "/".equals(redirect),
                "期望回到 index.html 或 SPM 首页，实际: " + redirect);
    }

    private JSONObject postLogin() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", sysUserService.getAdmin());
        form.add("password", "admin");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/sys/login",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class);
        assertEquals(200, response.getStatusCodeValue());
        return IntegrationJson.parse(response.getBody());
    }

    private static boolean looksLikeRestApiRedirect(String redirect) {
        if (redirect == null) return true;
        String lower = redirect.toLowerCase();
        return lower.contains("/sys/menu/nav")
                || lower.contains("/api/")
                || lower.endsWith(".json")
                || lower.contains("/me?")
                || lower.matches(".*/me$");
    }

    private static boolean looksLikeSafeHomeRedirect(String redirect) {
        return "index.html".equals(redirect)
                || "/".equals(redirect)
                || redirect.endsWith("/index.html");
    }
}
