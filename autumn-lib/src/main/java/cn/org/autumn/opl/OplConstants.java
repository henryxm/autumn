package cn.org.autumn.opl;

/**
 * OPL 开放平台常量：HTTP 路径、OAuth 参数、默认值、状态码与领域事件。
 * <p>
 * 与 {@link cn.org.autumn.opc.OpcConstants} 共用 {@code /open} 前缀；仅在与 OPC 路径冲突时插入命名空间 {@code opl}。
 */
public final class OplConstants {

    private OplConstants() {
    }

    // --- HTTP 路径 ---

    /** 开放能力 HTTP 根前缀 */
    public static final String OPEN = "/open";

    /** 本模块命名空间（仅用于冲突路径） */
    public static final String NS = "opl";

    /** OAuth2 AS 根路径（authorize / token / userInfo / login 等与 OPC 不冲突的端点） */
    public static final String OAUTH2_ROOT = OPEN + "/oauth2";

    /** 与 {@link #OAUTH2_ROOT} 相同 */
    public static final String OAUTH2_BASE = OAUTH2_ROOT;

    /** 授权页内登录 POST（与 OPC {@code /login} 冲突，故加 {@code opl}） */
    public static final String OAUTH2_LOGIN = OAUTH2_ROOT + "/" + NS + "/login";

    /** Open API 公共根 */
    public static final String API_V1_BASE = OPEN + "/api/v1";

    /** 平台侧开发者 Open API（与 OPC 同根；仅 {@code app/list} 等冲突方法单独加 {@code opl}） */
    public static final String API_PLATFORM = API_V1_BASE + "/platform";

    /** 管理 API 根路径 */
    public static final String ADMIN_BASE = OPEN + "/admin";

    /** 平台侧管理 API（与 OPC 管理端大量路径冲突，整树加 {@code opl}） */
    public static final String ADMIN_PLATFORM = ADMIN_BASE + "/" + NS + "/platform";

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
