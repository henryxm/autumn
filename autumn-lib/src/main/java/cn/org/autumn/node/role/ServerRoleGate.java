package cn.org.autumn.node.role;

import cn.org.autumn.config.Config;
import cn.org.autumn.node.NodeProfile;
import cn.org.autumn.node.ProfileService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * 本机服务器角色门禁（100% 前向兼容：空 roles 或 ALL = 全开）。
 */
public final class ServerRoleGate {

    private ServerRoleGate() {
    }

    /** 空 / 仅空白 / 含 ALL → 无限制。 */
    public static boolean isUnrestricted() {
        return isUnrestricted(currentRoles());
    }

    public static boolean isUnrestricted(Collection<String> roles) {
        List<String> normalized = ServerRoleGroups.normalize(roles);
        if (normalized.isEmpty()) {
            return true;
        }
        for (String r : normalized) {
            if (ServerRole.CODE_ALL.equalsIgnoreCase(r)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasCapability(String capability) {
        return hasCapability(currentRoles(), capability);
    }

    public static boolean hasCapability(Collection<String> roles, String capability) {
        if (StringUtils.isBlank(capability)) {
            return true;
        }
        if (isUnrestricted(roles)) {
            return true;
        }
        String want = capability.trim().toUpperCase();
        ServerRoleRegistry registry = registry();
        for (String code : ServerRoleGroups.normalize(roles)) {
            if (registry != null) {
                ServerRole role = registry.get(code);
                if (role != null && role.hasCapability(want)) {
                    return true;
                }
            }
            if (BuiltinRoleCapabilities.has(code, want)) {
                return true;
            }
            if (want.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * LoopJob / 注解所需角色：空 required → 允许；本机 unrestricted → 允许；否则需本机具备全部 required（或本机 ALL）。
     */
    public static boolean allowsAll(String... requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }
        boolean any = false;
        for (String r : requiredRoles) {
            if (StringUtils.isNotBlank(r)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return true;
        }
        List<String> mine = currentRoles();
        if (isUnrestricted(mine)) {
            return true;
        }
        for (String r : requiredRoles) {
            if (StringUtils.isBlank(r)) {
                continue;
            }
            if (!hasRole(mine, r.trim())) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasRole(String role) {
        return hasRole(currentRoles(), role);
    }

    public static boolean hasRole(Collection<String> roles, String role) {
        if (StringUtils.isBlank(role)) {
            return true;
        }
        if (isUnrestricted(roles)) {
            return true;
        }
        String want = role.trim();
        for (String r : ServerRoleGroups.normalize(roles)) {
            if (want.equalsIgnoreCase(r)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> currentRoles() {
        NodeProfile profile = bean(NodeProfile.class);
        if (profile == null) {
            ProfileService svc = bean(ProfileService.class);
            if (svc == null) {
                return List.of();
            }
            return new ArrayList<>(svc.roles());
        }
        return new ArrayList<>(profile.roles());
    }

    private static ServerRoleRegistry registry() {
        return bean(ServerRoleRegistry.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T bean(Class<T> type) {
        Object o = Config.getBean(type);
        return type.isInstance(o) ? (T) o : null;
    }
}
