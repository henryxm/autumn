package cn.org.autumn.modules.oauth.util;

import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
import java.security.Principal;
import java.util.*;

/**
 * 适配器：将 jakarta.servlet.http.HttpServletRequest 包装为 javax.servlet.http.HttpServletRequest
 * <p>
 * 用于兼容 Apache Oltu 等仍使用 javax.servlet 的旧版库。
 * 仅实现 Oltu OAuth2 库实际调用的方法，其余方法抛出 UnsupportedOperationException。
 */
public class JakartaToJavaxRequestAdapter implements javax.servlet.http.HttpServletRequest {

    private final HttpServletRequest delegate;

    public JakartaToJavaxRequestAdapter(HttpServletRequest delegate) {
        this.delegate = delegate;
    }

    // ---- HttpServletRequest methods used by Oltu ----

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public String getParameter(String name) {
        return delegate.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return delegate.getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return delegate.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return delegate.getParameterValues(name);
    }

    @Override
    public String getHeader(String name) {
        return delegate.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return delegate.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return delegate.getHeaderNames();
    }

    @Override
    public String getQueryString() {
        return delegate.getQueryString();
    }

    @Override
    public String getRequestURI() {
        return delegate.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return delegate.getRequestURL();
    }

    @Override
    public String getServletPath() {
        return delegate.getServletPath();
    }

    @Override
    public String getContextPath() {
        return delegate.getContextPath();
    }

    @Override
    public String getScheme() {
        return delegate.getScheme();
    }

    @Override
    public String getServerName() {
        return delegate.getServerName();
    }

    @Override
    public int getServerPort() {
        return delegate.getServerPort();
    }

    @Override
    public String getRemoteAddr() {
        return delegate.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return delegate.getRemoteHost();
    }

    @Override
    public int getRemotePort() {
        return delegate.getRemotePort();
    }

    @Override
    public String getLocalAddr() {
        return delegate.getLocalAddr();
    }

    @Override
    public String getLocalName() {
        return delegate.getLocalName();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public javax.servlet.ServletInputStream getInputStream() throws IOException {
        jakarta.servlet.ServletInputStream jakartaStream = delegate.getInputStream();
        return new javax.servlet.ServletInputStream() {
            @Override
            public int read() throws IOException {
                return jakartaStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return jakartaStream.read(b, off, len);
            }

            @Override
            public boolean isFinished() {
                return jakartaStream.isFinished();
            }

            @Override
            public boolean isReady() {
                return jakartaStream.isReady();
            }

            @Override
            public void setReadListener(javax.servlet.ReadListener readListener) {
                // no-op for Oltu compatibility
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return delegate.getReader();
    }

    @Override
    public String getCharacterEncoding() {
        return delegate.getCharacterEncoding();
    }

    @Override
    public int getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return delegate.getContentLengthLong();
    }

    @Override
    public Object getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object o) {
        delegate.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getProtocol() {
        return delegate.getProtocol();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public Locale getLocale() {
        return delegate.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return delegate.getLocales();
    }

    @Override
    public String getAuthType() {
        return delegate.getAuthType();
    }

    @Override
    public javax.servlet.http.Cookie[] getCookies() {
        jakarta.servlet.http.Cookie[] jakartaCookies = delegate.getCookies();
        if (jakartaCookies == null) return null;
        javax.servlet.http.Cookie[] javaxCookies = new javax.servlet.http.Cookie[jakartaCookies.length];
        for (int i = 0; i < jakartaCookies.length; i++) {
            javaxCookies[i] = new javax.servlet.http.Cookie(jakartaCookies[i].getName(), jakartaCookies[i].getValue());
        }
        return javaxCookies;
    }

    @Override
    public long getDateHeader(String name) {
        return delegate.getDateHeader(name);
    }

    @Override
    public int getIntHeader(String name) {
        return delegate.getIntHeader(name);
    }

    @Override
    public String getPathInfo() {
        return delegate.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return delegate.getPathTranslated();
    }

    @Override
    public String getRemoteUser() {
        return delegate.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
        return delegate.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return delegate.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return delegate.getRequestedSessionId();
    }

    @Override
    public javax.servlet.http.HttpSession getSession(boolean create) {
        throw new UnsupportedOperationException("Session not supported in adapter");
    }

    @Override
    public javax.servlet.http.HttpSession getSession() {
        throw new UnsupportedOperationException("Session not supported in adapter");
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return delegate.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return delegate.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return delegate.isRequestedSessionIdFromURL();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(javax.servlet.http.HttpServletResponse response) {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public void login(String username, String password) {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public Collection<javax.servlet.http.Part> getParts() {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public javax.servlet.http.Part getPart(String name) {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public void setCharacterEncoding(String env) throws java.io.UnsupportedEncodingException {
        delegate.setCharacterEncoding(env);
    }

    @Override
    public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public javax.servlet.ServletContext getServletContext() {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public javax.servlet.AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public javax.servlet.AsyncContext startAsync(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public boolean isAsyncStarted() {
        return delegate.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return delegate.isAsyncSupported();
    }

    @Override
    public javax.servlet.AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException("Not supported in adapter");
    }

    @Override
    public javax.servlet.DispatcherType getDispatcherType() {
        return javax.servlet.DispatcherType.valueOf(delegate.getDispatcherType().name());
    }

    /**
     * 工具方法：将 jakarta HttpServletRequest 转换为 javax HttpServletRequest
     */
    public static javax.servlet.http.HttpServletRequest adapt(HttpServletRequest jakartaRequest) {
        return new JakartaToJavaxRequestAdapter(jakartaRequest);
    }
}
