package cn.org.autumn.node.role;

import cn.org.autumn.config.Config;
import cn.org.autumn.job.JobDuty;
import cn.org.autumn.node.NodeProfile;
import cn.org.autumn.node.ProfileService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * 本机服务器角色门禁（100% 前向兼容：空 roles 或 ALL = 全开）。
 * <p>
 * LOCAL 专岗（含 {@link ServerRole#CODE_LOCAL}、不含 {@link ServerRole#CODE_JOB}、非 unrestricted）
 * 对 {@link JobDuty#SINGLETON}/{@link JobDuty#SEQUENTIAL} 走宽松跳过，见 {@link #allowsClusterJobDuty}。
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

    /**
     * LOCAL 专岗：已手动限制角色，且含 LOCAL、不含 JOB（含 ALL 时不算专岗）。
     * 空 roles / ALL → false（全开兼容）。
     */
    public static boolean isLocalScoped() {
        return isLocalScoped(currentRoles());
    }

    public static boolean isLocalScoped(Collection<String> roles) {
        if (isUnrestricted(roles)) {
            return false;
        }
        List<String> normalized = ServerRoleGroups.normalize(roles);
        boolean local = false;
        for (String r : normalized) {
            if (ServerRole.CODE_JOB.equalsIgnoreCase(r)) {
                return false;
            }
            if (ServerRole.CODE_LOCAL.equalsIgnoreCase(r)) {
                local = true;
            }
        }
        return local;
    }

    /**
     * 宽松集群编排门禁：LOCAL 专岗跳过 {@link JobDuty#SINGLETON}/{@link JobDuty#SEQUENTIAL}；
     * {@link JobDuty#ALL}/{@link JobDuty#LOCAL} 等仍允许。非专岗一律允许。
     */
    public static boolean allowsClusterJobDuty(JobDuty duty) {
        return allowsClusterJobDuty(currentRoles(), duty);
    }

    public static boolean allowsClusterJobDuty(Collection<String> roles, JobDuty duty) {
        if (duty == null || duty == JobDuty.ALL || duty == JobDuty.LOCAL || duty == JobDuty.DISABLED) {
            return true;
        }
        if (duty != JobDuty.SINGLETON && duty != JobDuty.SEQUENTIAL) {
            return true;
        }
        return !isLocalScoped(roles);
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
