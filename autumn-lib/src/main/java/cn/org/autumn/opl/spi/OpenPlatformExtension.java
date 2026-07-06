package cn.org.autumn.opl.spi;

import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenAuthorizationRequest;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;

/**
 * OPL 统一扩展点：校验、OAuth 生命周期、身份生成、userInfo  enrichment。
 * <p>
 * 业务项目 {@code @Component} 实现本接口，按 {@link org.springframework.core.annotation.Order} 升序调用；
 * 未覆盖的 default 方法不生效。
 */
public interface OpenPlatformExtension {

    default boolean supports(OpenAppType appType) {
        return true;
    }

    default void validateRegister(String accountUuid, String name, String redirectUri, String scope, OpenAppType appType) throws Exception {
    }

    default void validateRedirectUri(OpenAppSnapshot app, String redirectUri) throws Exception {
    }

    default void validateScope(OpenAppSnapshot app, String scope) throws Exception {
    }

    default void beforeAuthorizePage(OpenAppSnapshot app, OpenAuthorizationRequest request) throws Exception {
    }

    default void beforeApprove(OpenAppSnapshot app, OpenAuthorizationRequest request) throws Exception {
    }

    default void afterCodeIssued(OpenAppSnapshot app, OpenAuthorizationRequest request, String code) {
    }

    default void afterTokenIssued(OpenAppSnapshot app, OpenTokenSnapshot token) {
    }

    default void enrichUserInfo(OpenAppSnapshot app, OpenIdentitySnapshot identity, OpenUserInfoSnapshot userInfo) {
    }

    /** 是否跳过默认 redirectUri 精确匹配（如小程序多域策略） */
    default boolean relaxedRedirectMatch(OpenAppSnapshot app) {
        return false;
    }

    /** 返回非 null 时覆盖默认 openId 算法 */
    default String generateOpenId(OpenAppSnapshot app, String userUuid) {
        return null;
    }

    /** 返回非 null 时覆盖默认 unionId 算法 */
    default String generateUnionId(String accountUuid, String userUuid) {
        return null;
    }
}
