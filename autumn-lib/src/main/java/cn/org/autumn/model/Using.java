package cn.org.autumn.model;

import java.io.Serializable;

public class Using implements Serializable {
    private Boolean using;
    private String reason;

    public Boolean getUsing() {
        return using;
    }

    public void setUsing(Boolean using) {
        this.using = using;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Using() {
    }

    public Using(Boolean using) {
        this.using = using;
    }

    public Using(String reason) {
        this.reason = reason;
    }

    public Using(Boolean using, String reason) {
        this.using = using;
        this.reason = reason;
    }

    public boolean IndeedUsing() {
        return null != using && using;
    }
}
