package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全加固 · 运维接口 Token（SysConfig {@link #CONFIG_KEY}）。
 */
@Getter
@Setter
@ConfigParam(paramKey = SecurityOpsConfig.CONFIG_KEY, category = SecurityOpsConfig.config,
        name = "运维令牌", description = "运维清除/重初始化令牌，以及 Bean 执行器、WebShell、管理 IP 校验开关；令牌为空时相关危险操作一律拒绝")
public class SecurityOpsConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_OPS_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.StringType, name = "清除确认令牌",
            description = "危险操作二次确认令牌（如关闭管理 IP 校验、删除当前访问 IP 的白名单/管理 IP）；空则拒绝")
    private String clearToken = "";

    @ConfigField(category = InputType.StringType, name = "重初始化令牌",
            description = "运维重初始化接口认证令牌；空则拒绝")
    private String reinitToken = "";

    @ConfigField(category = InputType.BooleanType, name = "启用 Bean 执行器",
            description = "默认关闭：拒绝反射调用 Bean；开启后仍须系统管理员，并在开启管理 IP 校验时校验管理 IP，且记审计日志")
    private boolean beanExecuteEnabled = false;

    @ConfigField(category = InputType.BooleanType, name = "启用 WebShell",
            description = "默认关闭：拒绝 WebShell/Shell/SFTP；开启后仍须系统管理员与主机白名单，并在开启管理 IP 校验时校验管理 IP")
    private boolean webShellEnabled = false;

    @ConfigField(category = InputType.BooleanType, name = "启用管理IP校验",
            description = "默认关闭：后台管理仅校验系统管理员；开启后客户端 IP 还必须在「管理级 IP 白名单」中；账号泄露或遇攻击时建议开启")
    private boolean adminIpEnable = false;

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
}
