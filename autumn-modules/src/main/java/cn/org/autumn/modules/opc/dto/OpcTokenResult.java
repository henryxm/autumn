package cn.org.autumn.modules.opc.dto;

import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponse;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpcTokenResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;

    public static OpcTokenResult from(OAuthTokenResponse response) {
        OpcTokenResult result = new OpcTokenResult();
        if (response == null) {
            return result;
        }
        result.setAccessToken(response.getAccessToken());
        result.setRefreshToken(response.getRefreshToken());
        result.setTokenType(response.getTokenType());
        result.setExpiresIn(response.getExpiresIn());
        return result;
    }
}
