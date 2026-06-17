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

    /**
     * 使用状态已明确确认且不在使用中（可安全视为未引用）。
     */
    public boolean CertainlyNotUsing() {
        return Boolean.FALSE.equals(using);
    }

    /**
     * 使用状态无法确认（网络异常、响应无效等），删除流程应保守跳过。
     */
    public boolean Uncertain() {
        return null == using;
    }
}
