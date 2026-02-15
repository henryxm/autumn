package cn.org.autumn.site;

import cn.org.autumn.config.UsingHandler;
import cn.org.autumn.model.Using;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class UsingFactory extends Factory {

    public static String key = "autumn-using";

    @Autowired
    Gson gson;

    List<UsingHandler> list = null;

    public List<UsingHandler> getList() {
        if (null == list) {
            list = getOrderList(UsingHandler.class);
        }
        return list;
    }

    public int count() {
        if (null == list) {
            list = getOrderList(UsingHandler.class);
        }
        if (null != list)
            return list.size();
        return 0;
    }

    public boolean using(Object value) {
        if (null == list) {
            list = getOrderList(UsingHandler.class);
        }
        if (null == list || list.isEmpty())
            log.debug("无实例");
        for (UsingHandler handler : list) {
            if (handler.using(value))
                return true;
        }
        return false;
    }

    public Using using(String base, String value) {
        if (StringUtils.isBlank(base))
            return null;
        if (!base.startsWith("http://") && !base.startsWith("https://"))
            base = "https://" + base;
        if (!base.endsWith("/"))
            base += "/";
        String url = base + "sys/using";
        if (log.isDebugEnabled())
            log.debug("构建URL: base={}, 最终URL={}", base, url);
        String result = "";
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authentication", key);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            // 设置超时时间
            connection.setConnectTimeout(5000); // 连接超时5秒
            connection.setReadTimeout(10000);   // 读取超时10秒
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = value.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            // 读取响应
            int responseCode = connection.getResponseCode();
            if (log.isDebugEnabled())
                log.debug("HTTP请求响应码: {}, URL: {}", responseCode, url);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    result = response.toString();
                    if (log.isDebugEnabled())
                        log.debug("响应内容: {}", result);
                    if (null == gson)
                        gson = new Gson();
                    return gson.fromJson(result, Using.class);
                }
            } else {
                // 读取错误响应
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    if (log.isDebugEnabled())
                        log.debug("HTTP错误响应: {}, 错误内容: {}", responseCode, errorResponse);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("HTTP请求异常: URL={}, 错误信息={}, 请求结果:{}", url, e.getMessage(), result, e);
        }
        return null;
    }
}