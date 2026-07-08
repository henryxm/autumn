package cn.org.autumn.modules.sys.support;

import cn.org.autumn.utils.R;

/** 登录页 {@code POST /sys/autologin} 响应规则（供单测与 {@link cn.org.autumn.modules.sys.controller.SysLoginController} 复用）。 */
public final class SysAutologinSupport {

    public static final String REASON_SKIPPED = "skipped";
    public static final String REASON_AUTHENTICATED = "authenticated";
    public static final String REASON_SESSION_ACTIVE = "session_active";
    public static final String REASON_REMEMBER_ME_BLOCKED = "remember_me_blocked";
    public static final String REASON_DEV_PROBE = "dev_probe";
    public static final String REASON_AUTOLOGIN_DISABLED = "autologin_disabled";
    public static final String REASON_NONE = "none";

    private SysAutologinSupport() {
    }

    /**
     * 登录页 autologin 响应：{@code devAutologinEnabled=false}（默认）时不返回任何成功跳转；
     * 开启后仅 dev 环境且未登录时返回 {@code devProbe}，供静默 admin 探测。
     */
    public static R buildAutologinResponse(boolean skipMarked, boolean authenticated, boolean hasPrincipal, boolean devEnvironment, boolean devAutologinEnabled, String redirectUrl) {
        if (skipMarked) {
            return R.error().put("reason", REASON_SKIPPED);
        }
        if (!devAutologinEnabled) {
            if (authenticated) {
                return R.error().put("reason", REASON_SESSION_ACTIVE).put("sessionActiveHint", true);
            }
            if (hasPrincipal) {
                return R.error().put("reason", REASON_REMEMBER_ME_BLOCKED).put("rememberMeHint", true);
            }
            return R.error().put("reason", REASON_AUTOLOGIN_DISABLED);
        }
        if (authenticated) {
            return R.error().put("reason", REASON_SESSION_ACTIVE).put("sessionActiveHint", true);
        }
        if (hasPrincipal) {
            return R.error().put("reason", REASON_REMEMBER_ME_BLOCKED).put("rememberMeHint", true);
        }
        if (devEnvironment) {
            return R.ok().put("reason", REASON_DEV_PROBE).put("devProbe", true);
        }
        return R.error().put("reason", REASON_NONE);
    }
}
