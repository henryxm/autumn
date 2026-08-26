package cn.org.autumn.model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 客户端请求来源上下文：{@code X-Client-Source} 解析结果。
 * <p>
 * 格式：{@code <platform>.<feature>[.<subfeature>]}
 */
public class ClientSourceContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String raw;
    private String platform;
    private String feature;
    private String subfeature;
    private boolean batch;

    /** Dubbo / Hessian 反序列化需要 */
    public ClientSourceContext() {
        this.raw = "";
        this.platform = "";
        this.feature = "";
        this.subfeature = "";
        this.batch = false;
    }

    public ClientSourceContext(String raw, String platform, String feature, String subfeature, boolean batch) {
        this.raw = StringUtils.trimToEmpty(raw);
        this.platform = StringUtils.trimToEmpty(platform);
        this.feature = StringUtils.trimToEmpty(feature);
        this.subfeature = StringUtils.trimToEmpty(subfeature);
        this.batch = batch;
    }

    public static ClientSourceContext empty() {
        return new ClientSourceContext("", "", "", "", false);
    }

    public static ClientSourceContext of(String raw, boolean batch) {
        if (StringUtils.isBlank(raw)) {
            return new ClientSourceContext("", "", "", "", batch);
        }
        String trimmed = raw.trim();
        String[] parts = trimmed.split("\\.", 3);
        String platform = parts.length > 0 ? parts[0] : "";
        String feature = parts.length > 1 ? parts[1] : "";
        String subfeature = parts.length > 2 ? parts[2] : "";
        return new ClientSourceContext(trimmed, platform, feature, subfeature, batch);
    }

    public String getRaw() {
        return raw;
    }

    public String getPlatform() {
        return platform;
    }

    public String getFeature() {
        return feature;
    }

    public String getSubfeature() {
        return subfeature;
    }

    public boolean isBatch() {
        return batch;
    }

    public String sourceForLog() {
        return StringUtils.isNotBlank(raw) ? raw : "unknown";
    }

    public void setRaw(String raw) {
        this.raw = StringUtils.trimToEmpty(raw);
    }

    public void setPlatform(String platform) {
        this.platform = StringUtils.trimToEmpty(platform);
    }

    public void setFeature(String feature) {
        this.feature = StringUtils.trimToEmpty(feature);
    }

    public void setSubfeature(String subfeature) {
        this.subfeature = StringUtils.trimToEmpty(subfeature);
    }

    public void setBatch(boolean batch) {
        this.batch = batch;
    }
}
