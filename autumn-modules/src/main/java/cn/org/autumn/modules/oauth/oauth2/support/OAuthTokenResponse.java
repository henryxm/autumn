package cn.org.autumn.modules.oauth.oauth2.support;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * OAuth2 token 端点 JSON 响应解析结果（RP 侧共用）。
 */
@Getter
@Setter
public class OAuthTokenResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String rawBody;
}
