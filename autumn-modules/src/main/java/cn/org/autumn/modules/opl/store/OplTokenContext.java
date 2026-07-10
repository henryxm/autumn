package cn.org.autumn.modules.opl.store;

import java.io.Serializable;
import java.util.Date;

public class OplTokenContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appId;
    private String user;
    private String openId;
    private String unionId;
    private String authCode;
    private String accessToken;
    private String refreshToken;
    private String grantedScope;
    private Date expireAt;

    public OplTokenContext() {
    }

    public OplTokenContext(String appId, String user, String openId, String unionId) {
        this.appId = appId;
        this.user = user;
        this.openId = openId;
        this.unionId = unionId;
    }

    public long getExpireIn() {
        if (expireAt == null) {
            return 0L;
        }
        long expired = (expireAt.getTime() - System.currentTimeMillis()) / 1000 - 60;
        return expired < 0 ? 0L : expired;
    }

    public boolean isExpired() {
        return expireAt == null || !expireAt.after(new Date());
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Date getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Date expireAt) {
        this.expireAt = expireAt;
    }

    public String getGrantedScope() {
        return grantedScope;
    }

    public void setGrantedScope(String grantedScope) {
        this.grantedScope = grantedScope;
    }
}
