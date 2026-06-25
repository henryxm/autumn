package cn.org.autumn.modules.bot.support;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

@Slf4j
public final class RobotHookHttp {
    private RobotHookHttp() {
    }

    public static boolean postJson(String url, String json, Map<String, String> headers, int timeoutMs) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(1000)
                .setSocketTimeout(timeoutMs)
                .build();
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost post = new HttpPost(url);
            post.setConfig(config);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.setHeader(entry.getKey(), entry.getValue());
                }
            }
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            response = client.execute(post);
            int code = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());
            return code >= 200 && code < 300;
        } catch (Exception e) {
            log.debug("Hook HTTP failed: {}", e.getMessage());
            return false;
        } finally {
            try {
                if (response != null)
                    response.close();
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
