package cn.org.autumn.web;

/**
 * 请求身份的合法来源（以最终注入 Controller 的链路为准）。
 * <p>
 * 完整说明与用法见 {@code docs/AI_AUTH_CONTEXT.md}。
 */
public enum AuthSource {

    /** 纯 Shiro 会话，且无可用的同源 API OAuth 凭证 */
    SESSION,

    /** OAuth2 access_token；或 Session 由同源 API 令牌建立 */
    OAUTH_TOKEN,

    /** 业务 / 遗留 usr_user_token、UserTokenFactory */
    USER_TOKEN,

    /** client_credentials 影子账号（username=client_id）换真人 */
    ACCESS_KEY_SHADOW,

    /** Account 远程按 access_token 反查 */
    ACCESS_KEY_FROM_TOKEN,

    /** 仅 userUuid / user_uuid hint */
    UUID_HINT,

    /** 机器人 rbt_ 令牌或 RobotPrincipal 会话 */
    ROBOT_TOKEN,

    /** 未解析或匿名 */
    NONE
}
