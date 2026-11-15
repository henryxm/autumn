package cn.org.autumn.config;

/**
 * 机器人资料头像：对象存储 uuid（iconId）→ 展示 URL。
 * <p>
 * 多实现经 Factory 按 {@code @Order} 择优；调用方勿直接注入本类型。
 * 禁止在本接口上挂 {@code @Component} 空桩，否则 {@code hasIconUrlHandler()} 恒真却无法解析。
 * <p>
 * 基础框架不内嵌对象存储；业务仓引入 {@code minclouds-file-starter} 后由其 AutoConfig 注册实现。
 */
public interface BotIconUrlHandler {

    /**
     * @param objectUuid 对象存储 uuid（iconId）
     * @return 可访问展示 URL；无法解析时返回空串（勿返回 null）
     */
    String resolveAccessUrl(String objectUuid);
}
