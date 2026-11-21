package cn.org.autumn.modules.sys.support;

import cn.org.autumn.exception.AException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * clearToken / 临时安全密码：常量时间比较、最短长度、失败锁定。
 */
public final class ClearTokenSupport {

    public static final int MIN_LENGTH = 16;
    public static final int MAX_LENGTH = 256;
    private static final int MAX_FAILURES = 8;
    private static final long LOCK_MS = 15 * 60 * 1000L;

    private static final ConcurrentHashMap<String, FailureWindow> FAILURES = new ConcurrentHashMap<>();

    private ClearTokenSupport() {
    }

    public static void validateNewToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new AException("请填写清除确认令牌", 400);
        }
        if (token.length() < MIN_LENGTH || token.length() > MAX_LENGTH) {
            throw new AException("清除确认令牌长度须为 " + MIN_LENGTH + "～" + MAX_LENGTH, 400);
        }
        if (token.chars().anyMatch(Character::isWhitespace)) {
            throw new AException("清除确认令牌不能包含空白字符", 400);
        }
    }

    /**
     * 常量时间比较；失败累计锁定。{@code clientKey} 建议为 IP 或 session 标识。
     *
     * @return true 匹配成功
     */
    public static boolean matches(String configured, String provided, String clientKey) {
        assertNotLocked(clientKey);
        boolean ok = constantTimeEquals(configured, provided);
        if (ok) {
            clearFailures(clientKey);
            return true;
        }
        recordFailure(clientKey);
        return false;
    }

    public static void assertMatches(String configured, String provided, String clientKey) {
        if (StringUtils.isBlank(configured)) {
            throw new AException("清除确认令牌未配置", 403);
        }
        if (StringUtils.isBlank(provided) || !matches(configured, provided, clientKey)) {
            throw new AException("清除确认令牌不正确", 403);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    public static void assertNotLocked(String clientKey) {
        String key = StringUtils.defaultIfBlank(clientKey, "-");
        FailureWindow w = FAILURES.get(key);
        if (w != null && w.lockedUntilMs > System.currentTimeMillis()) {
            throw new AException("尝试次数过多，请稍后再试", 429);
        }
    }

    public static void clearFailures(String clientKey) {
        FAILURES.remove(StringUtils.defaultIfBlank(clientKey, "-"));
    }

    /** 测试用：清空失败窗口。 */
    public static void resetForTests() {
        FAILURES.clear();
    }

    private static void recordFailure(String clientKey) {
        String key = StringUtils.defaultIfBlank(clientKey, "-");
        long now = System.currentTimeMillis();
        prune(now);
        FAILURES.compute(key, (k, prev) -> {
            FailureWindow w = prev == null ? new FailureWindow() : prev;
            if (w.lockedUntilMs > now) {
                return w;
            }
            if (now - w.windowStartMs > LOCK_MS) {
                w.windowStartMs = now;
                w.failures.set(0);
            }
            int n = w.failures.incrementAndGet();
            if (n >= MAX_FAILURES) {
                w.lockedUntilMs = now + LOCK_MS;
            }
            return w;
        });
    }

    private static void prune(long now) {
        if (FAILURES.size() < 256) {
            return;
        }
        Iterator<Map.Entry<String, FailureWindow>> it = FAILURES.entrySet().iterator();
        while (it.hasNext()) {
            FailureWindow w = it.next().getValue();
            if (w.lockedUntilMs < now && now - w.windowStartMs > LOCK_MS * 2) {
                it.remove();
            }
        }
    }

    private static final class FailureWindow {
        final AtomicInteger failures = new AtomicInteger();
        volatile long windowStartMs = System.currentTimeMillis();
        volatile long lockedUntilMs;
    }
}
