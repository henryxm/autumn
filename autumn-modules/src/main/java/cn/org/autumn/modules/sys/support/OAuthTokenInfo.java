package cn.org.autumn.modules.sys.support;

import cn.org.autumn.modules.oauth.store.TokenStore;
import java.io.Serializable;

/**
 * 单次 OAuth TokenStore 解析结果，避免重复 isValid/get。
 */
public final class OAuthTokenInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userUuid;
    private final String clientId;
    private final TokenStore store;

    public OAuthTokenInfo(String userUuid, String clientId, TokenStore store) {
        this.userUuid = userUuid == null ? "" : userUuid;
        this.clientId = clientId == null ? "" : clientId;
        this.store = store;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public String getClientId() {
        return clientId;
    }

    public TokenStore getStore() {
        return store;
    }

    public boolean hasClientId() {
        return clientId != null && !clientId.isEmpty();
    }
}
