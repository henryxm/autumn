package cn.org.autumn.modules.oauth.store;

public enum ValueType {
    authCode("oauth2:auth_code:"),
    accessToken("oauth2:access_token:"),
    refreshToken("oauth2:refresh_token:");

    private String value;

    ValueType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
