package cn.org.autumn.config;

public enum ClientType {
    /** 站点内置默认客户端（sys_config clientId）；启动时仅确保存在并标记类型，不自动改写 OAuth 配置 */
    SiteDefault,
    /** 管理端/OAuth RP 手动创建；升级与域名变更时不自动修改 */
    ManualCreate,
    /** AccessKey 类客户端；一般不需要更改 */
    AccessKey,
}