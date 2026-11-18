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
 * 安全加固 · 认证与 CSRF/CORS（SysConfig {@link #CONFIG_KEY}）。
 */
@Getter
@Setter
@ConfigParam(paramKey = SecurityAuthConfig.CONFIG_KEY, category = SecurityAuthConfig.config,
        name = "安全认证配置", description = "超级密码、引导口令待办、口令升级、历史口令复用、hint、CSRF、CORS、后台门禁（集群统一）")
public class SecurityAuthConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_AUTH_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.BooleanType, name = "启用超级密码", description = "关闭时忽略 SUPER_PASSWORD；开启且超级密码≥20位时可用其登录任意账号（紧急通道，非日常初始口令）")
    private boolean superPasswordEnabled = false;

    @ConfigField(category = InputType.BooleanType, name = "登录惰性升级口令", description = "历史 SHA 用户密码登录成功后 CAS 升级为 PBKDF2；关闭则永不改写历史 password")
    private boolean passwordUpgradeOnLogin = true;

    @ConfigField(category = InputType.BooleanType, name = "允许复用历史口令", description = "开启（默认）：自助改密/忘记密码可设为当前或最近用过的口令；关闭：上述路径拒绝与当前口令及最近 5 次历史相同。管理端代设口令不受此约束")
    private boolean passwordReuseAllowed = true;

    @ConfigField(category = InputType.BooleanType, name = "允许仅 userUuid hint", description = "无 Token 时允许凭 userUuid 解析身份（仅内网/网关可信场景；默认关闭）")
    private boolean allowUserUuidHint = false;

    @ConfigField(category = InputType.BooleanType, name = "强制 CSRF", description = "Cookie 会话的 POST/PUT/DELETE/PATCH 须带 X-CSRF-Token；生产建议开启")
    private boolean csrfEnforce = false;

    @ConfigField(category = InputType.StringType, name = "CORS 允许 Origin", description = "额外允许的完整 Origin，逗号或换行分隔；空则仅站点同源")
    private String corsAllowedOrigins = "";

    @ConfigField(category = InputType.BooleanType, name = "引导口令待办", description = "未配置 system.password 时系统生成随机管理员口令后为 true；改密或「已知晓并保留」后清除；驱动管理端提示条")
    private boolean adminBootstrapPending = false;

    @ConfigField(category = InputType.StringType, name = "引导口令确认时间", description = "用户点击「已知晓并保留」时的 ISO-8601 时间；空表示未确认")
    private String bootstrapAcknowledgedAt = "";

    @ConfigField(category = InputType.BooleanType, name = "后台全量锁定", description = "关闭（默认）：非 gen 定制管理页可按菜单权限；开启：运维/gen/其它后台管理页均仅系统管理员。自助 API/页始终不锁；gen 全表与运维页无论开关均仅超管")
    private boolean adminLockdown = false;

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (corsAllowedOrigins == null) {
            corsAllowedOrigins = "";
        }
        if (bootstrapAcknowledgedAt == null) {
            bootstrapAcknowledgedAt = "";
        }
        return fixes;
    }

    public List<String> corsOriginList() {
        return SecurityConfigLists.splitList(corsAllowedOrigins);
    }
}
