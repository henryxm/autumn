package cn.org.autumn.modules.oauth.store;

import java.io.Serializable;
import java.util.Date;

public class TokenStore implements Serializable {
    Object value;
    String authCode;
    String accessToken;
    String refreshToken;
    Date date;

    public TokenStore(Object object) {
        this.value = object;
        this.date = new Date();
        this.date.setTime(this.date.getTime() + getExpireIn() * 1000);
    }

    public TokenStore(Object object, Long expire) {
        this.value = object;
        this.date = new Date();
        this.date.setTime(this.date.getTime() + expire * 1000);
    }

    public TokenStore(Object value, String authCode, String accessToken, String refreshToken, Long expire) {
        this.value = value;
        this.authCode = authCode;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.date = new Date();
        this.date.setTime(this.date.getTime() + expire * 1000);
    }

    public TokenStore(Object value, String authCode, String accessToken, String refreshToken, Date date) {
        this.value = value;
        this.authCode = authCode;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.date = date;
    }

    public long getExpireIn() {
        long expired = (this.date.getTime() - new Date().getTime()) / 1000;
        //设置提前一分钟过期，避免网络通信延时
        expired = expired - 60;
        if (expired < 0)
            expired = 0L;
        return expired;
    }

    public boolean isExpired() {
        return !date.after(new Date());
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}