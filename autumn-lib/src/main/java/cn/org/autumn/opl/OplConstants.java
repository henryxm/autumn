package cn.org.autumn.opl;

/**
 * OPL 开放平台常量：HTTP 路径、OAuth 参数、默认值、状态码与领域事件。
 * <p>
 * 业务扩展与 OPC 对接时统一引用本类，避免硬编码。
 */
public final class OplConstants {

    private OplConstants() {
    }

    // --- HTTP 路径 ---

    /** OAuth2 端点根路径（authorize / token / userInfo） */
    public static final String OAUTH2_BASE = "/opl/oauth2";

    /** 开发者自助 Open API 根路径 */
    public static final String API_V1_BASE = "/opl/api/v1";

    /** 系统管理员运维 API 根路径 */
    public static final String ADMIN_BASE = "/opl/admin";

    /** 统一管理页路由（模板 {@code opl/oplmanage.html}） */
    public static final String MANAGE_PAGE = "oplmanage.html";

    // --- OAuth 请求参数名 ---

    public static final String PARAM_APP_ID = "app_id";
    public static final String PARAM_APP_SECRET = "app_secret";

    // --- 默认值与 TTL（秒） ---

    /** 未指定 scope 时的默认值 */
    public static final String DEFAULT_SCOPE = "basic";

    /** 授权码有效期 */
    public static final long AUTH_CODE_TTL_SECONDS = 5 * 60L;

    /** access_token 有效期 */
    public static final long ACCESS_TOKEN_TTL_SECONDS = 24 * 60 * 60L;

    /** refresh_token 有效期 */
    public static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60L;

    // --- 状态（与模块内 Entity 字段取值一致） ---

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    /**
     * 领域事件名，供 {@link cn.org.autumn.opl.spi.OpenPlatformSubscriber#events()} 订阅。
     */
    public static final class Event {

        private Event() {
        }

        /** 订阅全部事件的通配符 */
        public static final String ALL = "*";

        public static final String APP_REGISTERED = "opl.app.registered";
        public static final String APP_SECRET_RESET = "opl.app.secret_reset";
        public static final String CODE_ISSUED = "opl.oauth.code_issued";
        public static final String TOKEN_ISSUED = "opl.oauth.token_issued";
        public static final String IDENTITY_RESOLVED = "opl.identity.resolved";
        public static final String UNION_CREATED = "opl.union.created";
    }
}
