package cn.org.autumn.model;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link ClientSourceContext} HTTP 解析；Dubbo attachment 透传由各业务模块自行实现（autumn-lib 不依赖 Dubbo）。
 */
public final class ClientSourceSupport {

    public static final String HEADER = "X-Client-Source";

    private ClientSourceSupport() {
    }

    public static ClientSourceContext from(HttpServletRequest request, boolean batch) {
        if (request == null) {
            return ClientSourceContext.empty();
        }
        return ClientSourceContext.of(request.getHeader(HEADER), batch);
    }
}
