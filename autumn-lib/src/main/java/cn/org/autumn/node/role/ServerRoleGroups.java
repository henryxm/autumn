package cn.org.autumn.node.role;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 角色组：描述可共存的角色集合（文档与校验辅助，非强制枚举）。
 */
@Component
public class ServerRoleGroups {

    public static final String GROUP_TRAFFIC = "traffic";
    public static final String GROUP_BACKEND = "backend";
    public static final String GROUP_OPS = "ops";

    private final Map<String, Set<String>> groups = new LinkedHashMap<>();

    public ServerRoleGroups() {
        groups.put(GROUP_TRAFFIC, Set.of(ServerRole.CODE_WEB, ServerRole.CODE_API));
        groups.put(GROUP_BACKEND, Set.of(ServerRole.CODE_WORKER, ServerRole.CODE_JOB));
        groups.put(GROUP_OPS, Set.of(ServerRole.CODE_MONITOR));
    }

    public void putGroup(String name, Set<String> roleCodes) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        Set<String> codes = new LinkedHashSet<>();
        if (roleCodes != null) {
            for (String c : roleCodes) {
                if (StringUtils.isNotBlank(c)) {
                    codes.add(c.trim().toUpperCase());
                }
            }
        }
        groups.put(name.trim().toLowerCase(), Collections.unmodifiableSet(codes));
    }

    public Set<String> getGroup(String name) {
        if (StringUtils.isBlank(name)) {
            return Set.of();
        }
        Set<String> g = groups.get(name.trim().toLowerCase());
        return g != null ? g : Set.of();
    }

    public Map<String, Set<String>> all() {
        return Collections.unmodifiableMap(groups);
    }

    /**
     * 规范化写入值：含 ALL → 仅 ALL；去空白、大写、去重保序。
     */
    public static java.util.List<String> normalize(java.util.Collection<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return java.util.List.of();
        }
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        boolean all = false;
        for (String r : raw) {
            if (StringUtils.isBlank(r)) {
                continue;
            }
            String c = r.trim().toUpperCase();
            if (ServerRole.CODE_ALL.equals(c)) {
                all = true;
                break;
            }
            set.add(c);
        }
        if (all) {
            return java.util.List.of(ServerRole.CODE_ALL);
        }
        return java.util.List.copyOf(set);
    }
}
