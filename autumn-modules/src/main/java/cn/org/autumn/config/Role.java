package cn.org.autumn.config;

/**
 * 权限与角色门面：业务代码仅依赖本接口注入。
 * <ul>
 *   <li>仅依赖 Autumn 时由 {@link StandardRole}（{@link DefaultRoleAutoConfiguration}）提供默认实现。</li>
 *   <li>引入 Minclouds 且存在 {@code MenuService} Bean 时由 {@code MincloudsRole} 覆盖（扩展菜单侧判定与缓存）。</li>
 * </ul>
 */
public interface Role {

    boolean isSystemAdmin(String userUuid);

    boolean isSystemAdmin();

    boolean isUserAdmin(String userUuid);

    boolean isUserAdmin();

    boolean isAgentAdmin(String userUuid);

    boolean isAgentAdmin();

    boolean hasRole(String roleId);
}
