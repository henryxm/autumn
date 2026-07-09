package cn.org.autumn.modules.sys.shiro;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.web.servlet.SimpleCookie;

/**
 * 按 {@link HostSessionCookieContext} 返回当前请求的 Cookie name/domain，线程安全。
 */
public class HostAwareSessionIdCookie extends SimpleCookie {

    public HostAwareSessionIdCookie(String name) {
        super(name);
    }

    @Override
    public String getName() {
        HostSessionCookieHolder holder = HostSessionCookieContext.get();
        if (holder != null && StringUtils.isNotBlank(holder.getName())) {
            return holder.getName();
        }
        return super.getName();
    }

    @Override
    public String getDomain() {
        HostSessionCookieHolder holder = HostSessionCookieContext.get();
        if (holder != null) {
            return holder.getDomain();
        }
        return super.getDomain();
    }
}
