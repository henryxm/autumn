package cn.org.autumn.node.role;

import java.util.Map;
import java.util.Set;

/** 预置角色能力快照（无 Spring Registry 时 Gate 兜底）。 */
final class BuiltinRoleCapabilities {

    private static final Map<String, Set<String>> MAP = Map.of(
            ServerRole.CODE_ALL, Set.of(ServerRole.CAP_ALL),
            ServerRole.CODE_WEB, Set.of(ServerRole.CAP_WEB_UI),
            ServerRole.CODE_API, Set.of(ServerRole.CAP_API_HTTP, ServerRole.CAP_FILE_DOWNLOAD),
            ServerRole.CODE_WORKER, Set.of(ServerRole.CAP_BACKGROUND),
            ServerRole.CODE_JOB, Set.of(ServerRole.CAP_SCHEDULED_JOB),
            ServerRole.CODE_MONITOR, Set.of(ServerRole.CAP_MONITOR));

    private BuiltinRoleCapabilities() {
    }

    static boolean has(String roleCode, String capability) {
        if (capability == null || capability.isBlank()) {
            return true;
        }
        Set<String> caps = MAP.get(roleCode == null ? "" : roleCode.trim().toUpperCase());
        if (caps == null) {
            return false;
        }
        if (caps.contains(ServerRole.CAP_ALL)) {
            return true;
        }
        return caps.contains(capability.trim().toUpperCase());
    }
}
