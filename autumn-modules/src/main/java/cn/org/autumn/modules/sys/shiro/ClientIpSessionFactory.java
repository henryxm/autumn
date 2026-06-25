package cn.org.autumn.modules.sys.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.web.session.mgt.WebSessionContext;
import org.apache.shiro.web.util.WebUtils;

/**
 * 新建 Shiro 会话时用 {@link IPUtils} 解析客户端 IP 作为 {@code Session#host}，替代 {@code request.getRemoteHost()}。
 */
public class ClientIpSessionFactory implements SessionFactory {

    @Override
    public Session createSession(SessionContext initData) {
        String ip = resolveClientIp(initData);
        if (StringUtils.isNotBlank(ip)) return new SimpleSession(ip);
        return new SimpleSession();
    }

    private static String resolveClientIp(SessionContext initData) {
        if (initData instanceof WebSessionContext) {
            WebSessionContext webCtx = (WebSessionContext) initData;
            ServletRequest request = webCtx.getServletRequest();
            if (request == null) request = WebUtils.getRequest(initData);
            if (request instanceof HttpServletRequest) return ClientIpSessionSupport.resolve((HttpServletRequest) request);
        }
        return initData != null ? StringUtils.trimToEmpty(initData.getHost()) : "";
    }
}
