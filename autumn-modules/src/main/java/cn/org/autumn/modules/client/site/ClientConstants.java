package cn.org.autumn.modules.client.site;

/** client 模块 OAuth 相关常量。 */
public final class ClientConstants {

    /** 经典 OAuth 自动注册本地用户时的用户名前缀 */
    public static final String OAUTH_AUTO_REGISTER_USERNAME_PREFIX = "oauth_";

    /** userInfo 传参：Autumn 历史 query JSON 包裹（同实例 AS 默认） */
    public static final String USERINFO_DELIVERY_LEGACY = "legacy";

    /** userInfo 传参：标准 Authorization Bearer（跨实例第三方 AS 默认） */
    public static final String USERINFO_DELIVERY_BEARER = "bearer";

    /** 自动注册用户名冲突时的最大重试次数（含首次） */
    public static final int OAUTH_AUTO_REGISTER_USERNAME_RETRY = 5;

    private ClientConstants() {
    }
}
