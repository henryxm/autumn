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
        name = "运维 Token", description = "clear/reinit 头认证 Token；Bean 执行开关；空 Token 则拒绝")
public class SecurityOpsConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_OPS_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.StringType, name = "运维 clear Token", description = "clear 接口头 x-clear-authentication；亦用于 wall 关白名单/删当前 IP 二次确认；空则拒绝")
    private String clearToken = "";

    @ConfigField(category = InputType.StringType, name = "运维 reinit Token", description = "reinit 接口头 x-reinit-authentication；空则拒绝")
    private String reinitToken = "";

    @ConfigField(category = InputType.BooleanType, name = "启用 Bean 执行器", description = "关闭（默认）：/sys/exec/bean/execute 拒绝反射调用；开启后仍须系统管理员 + IP 白名单，并记审计日志")
    private boolean beanExecuteEnabled = false;

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

    public boolean isBeanExecuteEnabled() {
        return beanExecuteEnabled;
    }

    public void setBeanExecuteEnabled(boolean beanExecuteEnabled) {
        this.beanExecuteEnabled = beanExecuteEnabled;
    }
}
