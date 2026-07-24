package cn.org.autumn.node.role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 全局服务器角色注册表：启动预置 + 运行时动态注册（业务可扩展）。
 */
@Slf4j
@Component
public class ServerRoleRegistry {

    private final ConcurrentHashMap<String, ServerRole> roles = new ConcurrentHashMap<>();

    public void register(ServerRole role) {
        if (role == null || StringUtils.isBlank(role.getCode())) {
            throw new IllegalArgumentException("role required");
        }
        ServerRole prev = roles.put(role.getCode(), role);
        if (prev != null && log.isDebugEnabled()) {
            log.debug("ServerRole replace code={}", role.getCode());
        } else if (log.isDebugEnabled()) {
            log.debug("ServerRole register code={}", role.getCode());
        }
    }

    public ServerRole get(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return roles.get(code.trim().toUpperCase());
    }

    public boolean contains(String code) {
        return get(code) != null;
    }

    public List<ServerRole> list() {
        List<ServerRole> list = new ArrayList<>(roles.values());
        list.sort(Comparator.comparingInt(ServerRole::getOrder).thenComparing(ServerRole::getCode));
        return Collections.unmodifiableList(list);
    }

    public Map<String, ServerRole> snapshot() {
        return Map.copyOf(roles);
    }

    /** 未知 code 返回 false；ALL 恒 true。 */
    public boolean isKnown(String code) {
        return contains(code);
    }
}
