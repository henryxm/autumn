package cn.org.autumn.modules.usr.dto;

public class VisitIp {
    String ip;
    boolean updated;

    public VisitIp(String ip) {
        this.ip = ip;
        this.updated = false;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
