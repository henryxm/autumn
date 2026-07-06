package cn.org.autumn.opc;

/**
 * OPC 开放接入常量：HTTP 路径、系统配置键与管理页。
 * <p>
 * 与 {@link cn.org.autumn.opl.OplConstants} 共用 {@code /open} 前缀；仅在与 OPL 路径冲突时插入命名空间 {@code opc}。
 */
public final class OpcConstants {

    private OpcConstants() {
    }

    /** 开放能力 HTTP 根前缀 */
    public static final String OPEN = "/open";

    /** 本模块命名空间（仅用于冲突路径） */
    public static final String NS = "opc";

    /** OAuth2 公共根路径（callback / login / success 等与 OPL 不冲突的端点） */
    public static final String OAUTH2_ROOT = OPEN + "/oauth2";

    /** 与 {@link #OAUTH2_ROOT} 相同；接入方非冲突端点挂在此根下 */
    public static final String OAUTH2_BASE = OAUTH2_ROOT;

    /** 接入方授权入口（与 OPL {@code /authorize} 冲突，故加 {@code opc}） */
    public static final String OAUTH2_AUTHORIZE = OAUTH2_ROOT + "/" + NS + "/authorize";

    /** Open API 公共根（与 OPL 大部分端点不冲突） */
    public static final String API_V1_BASE = OPEN + "/api/v1";

    /** 接入方自助 Open API */
    public static final String API_PLATFORM = API_V1_BASE + "/platform";

    /** 管理 API 根路径 */
    public static final String ADMIN_BASE = OPEN + "/admin";

    /** 接入方管理 API（与 OPL 管理端大量路径冲突，整树加 {@code opc}） */
    public static final String ADMIN_PLATFORM = ADMIN_BASE + "/" + NS + "/platform";

    /** Open 接入登录页路由（无 leading slash，供 {@code @RequestMapping}） */
    public static final String OAUTH2_LOGIN_PAGE = "open/oauth2/login";

    /** Open 接入登录成功页路由 */
    public static final String OAUTH2_SUCCESS_PAGE = "open/oauth2/success";

    /** 统一管理页路由（模板 {@code opc/opcmanage.html}） */
    public static final String MANAGE_PAGE = "opcmanage.html";

    /** {@code sys_config} 键：OAuth 回调无本地绑定时是否自动注册用户（默认 true） */
    public static final String CONFIG_AUTO_REGISTER = "OPC_AUTO_REGISTER";

    /** 自动注册本地用户时的用户名前缀 */
    public static final String AUTO_REGISTER_USERNAME_PREFIX = "opc_";
}
