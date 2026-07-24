package cn.org.autumn.node.role;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * 服务器角色元数据（非枚举）：code + 展示名 + 描述 + 能力集合。
 * <p>
 * 通过 {@link ServerRoleRegistry} 动态注册；框架预置见 {@link BuiltinServerRoles}。
 */
public final class ServerRole {

    public static final String CODE_ALL = "ALL";
    public static final String CODE_WEB = "WEB";
    public static final String CODE_API = "API";
    public static final String CODE_WORKER = "WORKER";
    public static final String CODE_JOB = "JOB";
    public static final String CODE_MONITOR = "MONITOR";

    public static final String CAP_ALL = "*";
    public static final String CAP_WEB_UI = "WEB_UI";
    public static final String CAP_API_HTTP = "API_HTTP";
    public static final String CAP_FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String CAP_BACKGROUND = "BACKGROUND";
    public static final String CAP_SCHEDULED_JOB = "SCHEDULED_JOB";
    public static final String CAP_MONITOR = "MONITOR";

    private final String code;
    private final String name;
    private final String description;
    private final Set<String> capabilities;
    private final int order;

    public ServerRole(String code, String name, String description, Set<String> capabilities, int order) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("role code blank");
        }
        this.code = code.trim().toUpperCase();
        this.name = StringUtils.defaultIfBlank(name, this.code);
        this.description = StringUtils.defaultString(description);
        Set<String> caps = new LinkedHashSet<>();
        if (capabilities != null) {
            for (String c : capabilities) {
                if (StringUtils.isNotBlank(c)) {
                    caps.add(c.trim().toUpperCase());
                }
            }
        }
        this.capabilities = Collections.unmodifiableSet(caps);
        this.order = order;
    }

    public static ServerRole of(String code, String name, String description, Set<String> capabilities) {
        return new ServerRole(code, name, description, capabilities, 100);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public int getOrder() {
        return order;
    }

    public boolean isAll() {
        return CODE_ALL.equals(code) || capabilities.contains(CAP_ALL);
    }

    public boolean hasCapability(String capability) {
        if (StringUtils.isBlank(capability)) {
            return true;
        }
        if (isAll()) {
            return true;
        }
        String want = capability.trim().toUpperCase();
        return capabilities.contains(want);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerRole that)) {
            return false;
        }
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "ServerRole{" + code + "}";
    }
}
