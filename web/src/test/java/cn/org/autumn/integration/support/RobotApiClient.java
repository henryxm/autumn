package cn.org.autumn.integration.support;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 机器人开放 API HTTP 客户端（{@code /bot/api/v1}）。
 */
public class RobotApiClient {

    public static final String API_PREFIX = "/bot/api/v1";

    private final TestRestTemplate rest;
    private final String baseUrl;

    public RobotApiClient(TestRestTemplate rest, String baseUrl) {
        this.rest = rest;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public JSONObject post(String path, String token, Map<String, Object> data) {
        String url = baseUrl + API_PREFIX + (path.startsWith("/") ? path : "/" + path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        applyToken(headers, token);
        Map<String, Object> body = new LinkedHashMap<>();
        if (data != null)
            body.put("data", data);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = rest.postForEntity(url, entity, String.class);
        return IntegrationJson.parse(response.getBody());
    }

    public JSONObject postWithHeaders(String path, String token, Map<String, Object> data, Map<String, String> extraHeaders) {
        String url = baseUrl + API_PREFIX + (path.startsWith("/") ? path : "/" + path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        applyToken(headers, token);
        if (extraHeaders != null) {
            for (Map.Entry<String, String> e : extraHeaders.entrySet()) {
                if (e.getValue() != null)
                    headers.set(e.getKey(), e.getValue());
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (data != null)
            body.put("data", data);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = rest.postForEntity(url, entity, String.class);
        return IntegrationJson.parse(response.getBody());
    }

    private static void applyToken(HttpHeaders headers, String token) {
        if (token == null || token.isEmpty())
            return;
        if (token.startsWith("rbt_"))
            headers.set("X-Robot-Token", token);
        else
            headers.set("Token", token);
    }
}
