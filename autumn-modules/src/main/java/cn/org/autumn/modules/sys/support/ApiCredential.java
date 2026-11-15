package cn.org.autumn.modules.sys.support;

import java.io.Serializable;

/**
 * API 请求头中解析出的凭证：规范化后的令牌 + 命中的头名。
 */
public final class ApiCredential implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String token;
    private final String header;

    public ApiCredential(String token, String header) {
        this.token = token == null ? "" : token;
        this.header = header == null ? "" : header;
    }

    public String getToken() {
        return token;
    }

    public String getHeader() {
        return header;
    }

    public boolean isPresent() {
        return token != null && !token.isEmpty();
    }
}
