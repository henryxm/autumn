package cn.org.autumn.opl.model;

/**
 * 开放平台应用类型（存储与 API 均使用枚举名）。
 */
public enum OpenAppType {
    Web,
    Android,
    Ios,
    HarmonyOS,
    OfficialAccount,
    ServiceAccount,
    MiniProgram;

    /** 管理端与文档展示用中文标签。 */
    public String getLabel() {
        switch (this) {
            case Android:
                return "安卓";
            case Ios:
                return "苹果";
            case HarmonyOS:
                return "鸿蒙";
            case OfficialAccount:
                return "公众号";
            case ServiceAccount:
                return "服务号";
            case MiniProgram:
                return "小程序";
            case Web:
            default:
                return "网页";
        }
    }

    public static OpenAppType parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Web;
        }
        String normalized = raw.trim();
        for (OpenAppType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        if ("ios".equalsIgnoreCase(normalized) || "iphone".equalsIgnoreCase(normalized)) {
            return Ios;
        }
        if ("harmony".equalsIgnoreCase(normalized) || "harmonyos".equalsIgnoreCase(normalized)) {
            return HarmonyOS;
        }
        if ("official".equalsIgnoreCase(normalized) || "official_account".equalsIgnoreCase(normalized) || "mp".equalsIgnoreCase(normalized) || "wechat_mp".equalsIgnoreCase(normalized) || "wechatmp".equalsIgnoreCase(normalized)) {
            return OfficialAccount;
        }
        if ("service".equalsIgnoreCase(normalized) || "service_account".equalsIgnoreCase(normalized) || "wechat_service".equalsIgnoreCase(normalized) || "wechatservice".equalsIgnoreCase(normalized)) {
            return ServiceAccount;
        }
        if ("mini_program".equalsIgnoreCase(normalized) || "miniprogram".equalsIgnoreCase(normalized)) {
            return MiniProgram;
        }
        throw new IllegalArgumentException("无效的应用类型: " + raw);
    }
}
