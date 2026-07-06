package cn.org.autumn.modules.opl.service;

import cn.org.autumn.utils.IPUtils;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/** OPL OAuth token 端点限流（本机滑动窗口）。 */
@Component
public class OplOAuthRateLimiter {

    private static final int DEFAULT_MAX_PER_MINUTE = 30;
    private static final ConcurrentHashMap<String, Window> LOCAL_WINDOWS = new ConcurrentHashMap<>();

    public void check(HttpServletRequest request, String appId) {
        if (StringUtils.isBlank(appId)) {
            return;
        }
        String ip = request == null ? "unknown" : StringUtils.defaultIfBlank(IPUtils.getIp(request), "unknown");
        String key = ip + ":" + appId.trim();
        long now = System.currentTimeMillis();
        Window window = LOCAL_WINDOWS.computeIfAbsent(key, k -> new Window());
        synchronized (window) {
            if (now - window.windowStartMs > 60_000L) {
                window.windowStartMs = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > DEFAULT_MAX_PER_MINUTE) {
                throw new IllegalStateException("请求过于频繁，请稍后再试");
            }
        }
    }

    private static final class Window {
        private long windowStartMs = System.currentTimeMillis();
        private int count;
    }
}
