package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全加固 · 运维接口 Token（SysConfig {@link #CONFIG_KEY}）。
 */
@ConfigParam(paramKey = SecurityOpsConfig.CONFIG_KEY, category = SecurityOpsConfig.config,
        name = "运维 Token", description = "clear/reinit 头认证 Token；空则拒绝")
public class SecurityOpsConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_OPS_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.StringType, name = "运维 clear Token", description = "clear 接口头 x-clear-authentication；空则拒绝")
    private String clearToken = "";

    @ConfigField(category = InputType.StringType, name = "运维 reinit Token", description = "reinit 接口头 x-reinit-authentication；空则拒绝")
    private String reinitToken = "";

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (clearToken == null) {
            clearToken = "";
        }
        if (reinitToken == null) {
            reinitToken = "";
        }
        return fixes;
    }

    public String getClearToken() {
        return clearToken;
    }

    public void setClearToken(String clearToken) {
        this.clearToken = clearToken;
    }

    public String getReinitToken() {
        return reinitToken;
    }

    public void setReinitToken(String reinitToken) {
        this.reinitToken = reinitToken;
    }
}
