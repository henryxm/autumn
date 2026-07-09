package cn.org.autumn.modules.sys.shiro;

/**
 * 单次 HTTP 请求解析出的 Shiro SessionId Cookie 参数。
 */
public final class HostSessionCookieHolder {

    private final String name;
    private final String domain;

    public HostSessionCookieHolder(String name, String domain) {
        this.name = name;
        this.domain = domain;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }
}
