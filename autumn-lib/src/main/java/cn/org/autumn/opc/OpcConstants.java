package cn.org.autumn.opc;

/**
 * OPC 开放接入常量：HTTP 路径、系统配置键与管理页。
 * <p>
 * OAuth 参数名与默认 scope 与 {@link cn.org.autumn.opl.OplConstants} 对齐（对接远程 OPL）。
 */
public final class OpcConstants {

    private OpcConstants() {
    }

    /** OAuth2 客户端端点根路径（authorize / callback） */
    public static final String OAUTH2_BASE = "/opc/oauth2";

    /** 用户自助 API 根路径 */
    public static final String API_V1_BASE = "/opc/api/v1";

    /** 系统管理员运维 API 根路径 */
    public static final String ADMIN_BASE = "/opc/admin";

    /** 统一管理页路由（模板 {@code opc/opcmanage.html}） */
    public static final String MANAGE_PAGE = "opcmanage.html";

    /** {@code sys_config} 键：OAuth 回调无本地绑定时是否自动注册用户（默认 true） */
    public static final String CONFIG_AUTO_REGISTER = "OPC_AUTO_REGISTER";

    /** 自动注册本地用户时的用户名前缀 */
    public static final String AUTO_REGISTER_USERNAME_PREFIX = "opc_";
}
