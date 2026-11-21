package cn.org.autumn.site;

import cn.org.autumn.config.GsonConfig;
import cn.org.autumn.config.UsingHandler;
import cn.org.autumn.model.Using;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 聚合本地 {@link UsingHandler}，并支持按基址远程探测 {@code /sys/using}。
 * <p>
 * 远程失败（5xx/超时等）返回 {@link Using#Uncertain()}，调用方须保守跳过删除。
 * 同一 URL 的失败 WARN 有冷却，避免日扫全表时每个 hash 刷屏。
 */
@Slf4j
@Component
public class UsingFactory extends Factory {

    public static String key = "autumn-using";

    /** 同一探测 URL 失败 WARN 的最小间隔（毫秒）。 */
    private static final long REMOTE_FAIL_WARN_COOLDOWN_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, Long> remoteFailWarnAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> remoteFailSuppressed = new ConcurrentHashMap<>();

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
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = value.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
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
                        gson = GsonConfig.getGson();
                    Using using = gson.fromJson(result, Using.class);
                    if (null == using || using.Uncertain()) {
                        warnRemoteFail(url, "响应无效: " + abbreviate(result, 200));
                        return new Using(null, "响应无效");
                    }
                    clearRemoteFail(url);
                    return using;
                }
            } else {
                String errorBody = readQuietly(connection.getErrorStream());
                warnRemoteFail(url, "code=" + responseCode + ", body=" + abbreviate(errorBody, 200));
            }
        } catch (Exception e) {
            warnRemoteFail(url, "错误: " + e.getMessage());
        }
        return new Using(null, "网络或响应异常");
    }

    private void warnRemoteFail(String url, String detail) {
        long now = System.currentTimeMillis();
        Long last = remoteFailWarnAt.get(url);
        if (last == null || now - last >= REMOTE_FAIL_WARN_COOLDOWN_MS) {
            AtomicInteger suppressed = remoteFailSuppressed.remove(url);
            int n = suppressed == null ? 0 : suppressed.get();
            if (n > 0) {
                log.warn("检查错误: URL={}, {}（冷却期内另有 {} 次同类失败已降为 debug）", url, detail, n);
            } else {
                log.warn("检查错误: URL={}, {}", url, detail);
            }
            remoteFailWarnAt.put(url, now);
            remoteFailSuppressed.put(url, new AtomicInteger(0));
        } else {
            remoteFailSuppressed.computeIfAbsent(url, k -> new AtomicInteger(0)).incrementAndGet();
            if (log.isDebugEnabled()) {
                log.debug("检查错误(冷却): URL={}, {}", url, detail);
            }
        }
    }

    private void clearRemoteFail(String url) {
        remoteFailWarnAt.remove(url);
        remoteFailSuppressed.remove(url);
    }

    private static String readQuietly(InputStream in) {
        if (in == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\r', ' ').replace('\n', ' ');
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
