package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全加固 · HTTP 透明代理出站（SysConfig {@link #CONFIG_KEY}）。
 */
@ConfigParam(paramKey = SecurityHttpProxyConfig.CONFIG_KEY, category = SecurityHttpProxyConfig.config,
        name = "HTTP 代理安全", description = "代理开关、DNS 校验、上游主机白名单（空=拒绝全部）")
public class SecurityHttpProxyConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_HTTP_PROXY_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.BooleanType, name = "启用 HTTP 代理", description = "关闭后 /http/proxy 一律拒绝")
    private boolean enabled = true;

    @ConfigField(category = InputType.BooleanType, name = "代理解析 DNS", description = "解析 DNS 后校验是否落在私网/元数据地址")
    private boolean resolveDns = true;

    @ConfigField(category = InputType.StringType, name = "代理允许主机", description = "上游主机白名单，逗号或换行分隔；支持 example.com / *.example.com；空=拒绝全部")
    private String allowedHosts = "";

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (allowedHosts == null) {
            allowedHosts = "";
        }
        return fixes;
    }

    public List<String> hostList() {
        return SecurityConfigLists.splitList(allowedHosts);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isResolveDns() {
        return resolveDns;
    }

    public void setResolveDns(boolean resolveDns) {
        this.resolveDns = resolveDns;
    }

    public String getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(String allowedHosts) {
        this.allowedHosts = allowedHosts;
    }
}
