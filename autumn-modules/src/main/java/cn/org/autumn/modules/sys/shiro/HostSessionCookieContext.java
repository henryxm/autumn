package cn.org.autumn.modules.sys.shiro;

/**
 * 当前请求线程的 Session Cookie 解析结果，供 {@link HostAwareSessionIdCookie} 读取，避免 mutating 全局 Cookie 单例。
 */
public final class HostSessionCookieContext {

    private static final ThreadLocal<HostSessionCookieHolder> HOLDER = new ThreadLocal<>();

    private HostSessionCookieContext() {
    }

    public static void set(HostSessionCookieHolder holder) {
        if (holder == null) {
            HOLDER.remove();
        } else {
            HOLDER.set(holder);
        }
    }

    public static HostSessionCookieHolder get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
