package cn.org.autumn.integration.auth;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.integration.support.IntegrationJson;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.alibaba.fastjson2.JSONObject;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 默认 AdminLTE 后台壳（index/main）仅系统管理员可渲染；非管理员应 404。
 */
@ActiveProfiles({"it", "h2"})
public class AdminShellAccessIntegrationTest extends IntegrationTest {

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void adminCanOpenIndexShell() {
        loginAs(sysUserService.getAdmin(), "admin");
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/index.html", String.class);
        assertEquals(200, response.getStatusCodeValue());
        String body = response.getBody() == null ? "" : response.getBody();
        assertFalse(body.contains("404") && body.length() < 500, "管理员不应落到短 404 页");
    }

    @Test
    void nonAdminGets404OnIndexAndMain() {
        String username = "shell_user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String password = "Passw0rd!";
        SysUserEntity user = sysUserService.newUser(username, null, password, Collections.emptyList());
        assertFalse(user == null);
        loginAs(username, password);

        ResponseEntity<String> index = restTemplate.getForEntity(baseUrl() + "/index.html", String.class);
        assertEquals(404, index.getStatusCodeValue());

        ResponseEntity<String> main = restTemplate.getForEntity(baseUrl() + "/main.html", String.class);
        assertEquals(404, main.getStatusCodeValue());
    }

    private void loginAs(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/sys/login",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class);
        assertEquals(200, response.getStatusCodeValue());
        JSONObject login = IntegrationJson.parse(response.getBody());
        IntegrationJson.assertSuccess(login);
        assertTrue(login.getString("data") != null && !login.getString("data").isBlank());
    }
}
