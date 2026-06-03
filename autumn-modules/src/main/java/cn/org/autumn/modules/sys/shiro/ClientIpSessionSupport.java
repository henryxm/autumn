package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;

import javax.servlet.http.HttpServletRequest;

/**
 * Shiro 会话「访问来源」与 {@link IPUtils#getIp(HttpServletRequest)} 对齐，避免 Docker / 反代后仅记录容器内网 IP。
 */
public final class ClientIpSessionSupport {

    /** 会话展示用客户端 IP（优先于 {@link Session#getHost()}）。 */
    public static final String CLIENT_IP_ATTR = "autumn.session.clientIp";

    private ClientIpSessionSupport() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) return "";
        return StringUtils.trimToEmpty(IPUtils.getIp(request));
    }

    public static void syncHost(Session session, HttpServletRequest request) {
        if (session == null || request == null) return;
        String ip = resolve(request);
        if (StringUtils.isBlank(ip)) return;
        session.setAttribute(CLIENT_IP_ATTR, ip);
        if (session instanceof SimpleSession) {
            SimpleSession ss = (SimpleSession) session;
            if (!ip.equals(StringUtils.defaultString(ss.getHost()))) ss.setHost(ip);
        }
    }

    public static String displayHost(Session session) {
        if (session == null) return "";
        Object attr = session.getAttribute(CLIENT_IP_ATTR);
        if (attr != null && StringUtils.isNotBlank(attr.toString())) return attr.toString().trim();
        return session.getHost() != null ? session.getHost() : "";
    }
}
