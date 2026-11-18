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
 * 安全加固 · Session/RememberMe Cookie 与 AES 密钥（SysConfig {@link #CONFIG_KEY}）。
 * <p>
 * Cookie 标志修改后需重启节点；密钥改后旧记住我失效。
 */
@Getter
@Setter
@ConfigParam(paramKey = SecuritySessionConfig.CONFIG_KEY, category = SecuritySessionConfig.config,
        name = "会话与 RememberMe", description = "Cookie Secure/SameSite 与 RememberMe AES 密钥（集群统一）")
public class SecuritySessionConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_SESSION_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.BooleanType, name = "RememberMe Cookie Secure", description = "生产 HTTPS 建议开启；修改后需重启")
    private boolean rememberMeSecure = false;

    @ConfigField(category = InputType.StringType, name = "RememberMe SameSite", description = "NONE / LAX / STRICT；修改后需重启")
    private String rememberMeSameSite = "LAX";

    @ConfigField(category = InputType.BooleanType, name = "Session Cookie Secure", description = "生产 HTTPS 建议开启；修改后需重启")
    private boolean sessionSecure = false;

    @ConfigField(category = InputType.StringType, name = "Session SameSite", description = "NONE / LAX / STRICT；修改后需重启")
    private String sessionSameSite = "LAX";

    @ConfigField(category = InputType.StringType, name = "RememberMe AES 密钥", description = "Base64 且解码后恰好 16 字节；空则 Redis SETNX / 本地文件。集群建议在此统一")
    private String rememberMeCipherKey = "";

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        rememberMeSameSite = SecurityConfigLists.normalizeSameSite(rememberMeSameSite, "LAX", fixes, "RememberMe SameSite");
        sessionSameSite = SecurityConfigLists.normalizeSameSite(sessionSameSite, "LAX", fixes, "Session SameSite");
        if (rememberMeCipherKey == null) {
            rememberMeCipherKey = "";
        } else {
            rememberMeCipherKey = rememberMeCipherKey.trim();
        }
        return fixes;
    }
}
