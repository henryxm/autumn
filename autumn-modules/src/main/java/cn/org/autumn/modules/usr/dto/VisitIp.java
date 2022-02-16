package cn.org.autumn.modules.usr.dto;

public class VisitIp {
    String ip;
    String userAgent;
    boolean updated;

    public VisitIp(String ip) {
        this.ip = ip;
        this.updated = false;
    }

    public VisitIp(String ip, String userAgent) {
        this.ip = ip;
        this.userAgent = userAgent;
        this.updated = false;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
