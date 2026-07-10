package cn.org.autumn.modules.client.oauth2;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;

/** 入站 Webhook 完成登录时使用的最小 HttpServletRequest，委托未覆盖方法至空壳请求。 */
public class RpQrcSyntheticRequest extends HttpServletRequestWrapper {

    private final Map<String, String> parameters = new HashMap<>();
    private final String hostHeader;
    private final String scheme;
    private final int serverPort;
    private final String remoteAddr = "127.0.0.1";

    public RpQrcSyntheticRequest(String callback, String baseUrl) {
        super(new EmptyHttpServletRequest());
        if (StringUtils.isNotBlank(callback)) {
            parameters.put("callback", callback);
        }
        String normalized = StringUtils.defaultString(baseUrl).trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String parsedHost = "localhost";
        String parsedScheme = "http";
        int parsedPort = 80;
        if (StringUtils.isNotBlank(normalized)) {
            try {
                java.net.URI uri = java.net.URI.create(normalized);
                if (StringUtils.isNotBlank(uri.getScheme())) {
                    parsedScheme = uri.getScheme();
                }
                if (StringUtils.isNotBlank(uri.getHost())) {
                    parsedHost = uri.getHost();
                }
                int uriPort = uri.getPort();
                if (uriPort > 0) {
                    parsedPort = uriPort;
                } else if ("https".equalsIgnoreCase(parsedScheme)) {
                    parsedPort = 443;
                }
            } catch (Exception ignored) {
                parsedHost = normalized.replaceFirst("^https?://", "");
                int slash = parsedHost.indexOf('/');
                if (slash >= 0) {
                    parsedHost = parsedHost.substring(0, slash);
                }
            }
        }
        this.scheme = parsedScheme;
        this.hostHeader = parsedPort == 80 || parsedPort == 443 ? parsedHost : parsedHost + ":" + parsedPort;
        this.serverPort = parsedPort;
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public String getHeader(String name) {
        if ("host".equalsIgnoreCase(name)) {
            return hostHeader;
        }
        return null;
    }

    @Override
    public String getServerName() {
        int colon = hostHeader.indexOf(':');
        return colon >= 0 ? hostHeader.substring(0, colon) : hostHeader;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public boolean isSecure() {
        return "https".equalsIgnoreCase(scheme);
    }

    @Override
    public String getRequestURI() {
        return "/client/oauth2/qrc/web/inbound";
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(scheme + "://" + hostHeader + getRequestURI());
    }

    @Override
    public String getMethod() {
        return "POST";
    }

    private static final class EmptyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        EmptyHttpServletRequest() {
            super(new jakarta.servlet.http.HttpServletRequest() {
                @Override
                public String getAuthType() {
                    return null;
                }

                @Override
                public jakarta.servlet.http.Cookie[] getCookies() {
                    return new jakarta.servlet.http.Cookie[0];
                }

                @Override
                public long getDateHeader(String name) {
                    return -1;
                }

                @Override
                public String getHeader(String name) {
                    return null;
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    return Collections.emptyEnumeration();
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    return Collections.emptyEnumeration();
                }

                @Override
                public int getIntHeader(String name) {
                    return -1;
                }

                @Override
                public String getMethod() {
                    return "GET";
                }

                @Override
                public String getPathInfo() {
                    return null;
                }

                @Override
                public String getPathTranslated() {
                    return null;
                }

                @Override
                public String getContextPath() {
                    return "";
                }

                @Override
                public String getQueryString() {
                    return null;
                }

                @Override
                public String getRemoteUser() {
                    return null;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return false;
                }

                @Override
                public java.security.Principal getUserPrincipal() {
                    return null;
                }

                @Override
                public String getRequestedSessionId() {
                    return null;
                }

                @Override
                public String getRequestURI() {
                    return "/";
                }

                @Override
                public StringBuffer getRequestURL() {
                    return new StringBuffer("http://localhost/");
                }

                @Override
                public String getServletPath() {
                    return "/";
                }

                @Override
                public jakarta.servlet.ServletConnection getServletConnection() {
                    return null;
                }

                @Override
                public String getRequestId() {
                    return null;
                }

                @Override
                public String getProtocolRequestId() {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpSession getSession(boolean create) {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpSession getSession() {
                    return null;
                }

                @Override
                public String changeSessionId() {
                    return null;
                }

                @Override
                public boolean isRequestedSessionIdValid() {
                    return false;
                }

                @Override
                public boolean isRequestedSessionIdFromCookie() {
                    return false;
                }

                @Override
                public boolean isRequestedSessionIdFromURL() {
                    return false;
                }

                @Override
                public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) {
                    return false;
                }

                @Override
                public void login(String username, String password) {
                }

                @Override
                public void logout() {
                }

                @Override
                public java.util.Collection<jakarta.servlet.http.Part> getParts() {
                    return Collections.emptyList();
                }

                @Override
                public jakarta.servlet.http.Part getPart(String name) {
                    return null;
                }

                @Override
                public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
                    return null;
                }

                @Override
                public Object getAttribute(String name) {
                    return null;
                }

                @Override
                public Enumeration<String> getAttributeNames() {
                    return Collections.emptyEnumeration();
                }

                @Override
                public String getCharacterEncoding() {
                    return "UTF-8";
                }

                @Override
                public void setCharacterEncoding(String env) {
                }

                @Override
                public int getContentLength() {
                    return 0;
                }

                @Override
                public long getContentLengthLong() {
                    return 0;
                }

                @Override
                public String getContentType() {
                    return null;
                }

                @Override
                public jakarta.servlet.ServletInputStream getInputStream() {
                    return null;
                }

                @Override
                public String getParameter(String name) {
                    return null;
                }

                @Override
                public Enumeration<String> getParameterNames() {
                    return Collections.emptyEnumeration();
                }

                @Override
                public String[] getParameterValues(String name) {
                    return null;
                }

                @Override
                public Map<String, String[]> getParameterMap() {
                    return Collections.emptyMap();
                }

                @Override
                public String getProtocol() {
                    return "HTTP/1.1";
                }

                @Override
                public String getScheme() {
                    return "http";
                }

                @Override
                public String getServerName() {
                    return "localhost";
                }

                @Override
                public int getServerPort() {
                    return 80;
                }

                @Override
                public java.io.BufferedReader getReader() {
                    return null;
                }

                @Override
                public String getRemoteAddr() {
                    return "127.0.0.1";
                }

                @Override
                public String getRemoteHost() {
                    return "127.0.0.1";
                }

                @Override
                public void setAttribute(String name, Object o) {
                }

                @Override
                public void removeAttribute(String name) {
                }

                @Override
                public java.util.Locale getLocale() {
                    return java.util.Locale.getDefault();
                }

                @Override
                public Enumeration<java.util.Locale> getLocales() {
                    return Collections.enumeration(Collections.singletonList(getLocale()));
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) {
                    return null;
                }

                @Override
                public int getRemotePort() {
                    return 0;
                }

                @Override
                public String getLocalName() {
                    return "localhost";
                }

                @Override
                public String getLocalAddr() {
                    return "127.0.0.1";
                }

                @Override
                public int getLocalPort() {
                    return 80;
                }

                @Override
                public jakarta.servlet.ServletContext getServletContext() {
                    return null;
                }

                @Override
                public jakarta.servlet.AsyncContext startAsync() {
                    return null;
                }

                @Override
                public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) {
                    return null;
                }

                @Override
                public boolean isAsyncStarted() {
                    return false;
                }

                @Override
                public boolean isAsyncSupported() {
                    return false;
                }

                @Override
                public jakarta.servlet.AsyncContext getAsyncContext() {
                    return null;
                }

                @Override
                public jakarta.servlet.DispatcherType getDispatcherType() {
                    return jakarta.servlet.DispatcherType.REQUEST;
                }
            });
        }
    }
}
