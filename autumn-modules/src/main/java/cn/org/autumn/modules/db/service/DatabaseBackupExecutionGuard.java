package cn.org.autumn.modules.db.service;

import cn.org.autumn.site.InitFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 集群下指定<strong>不参与备份</strong>的节点。
 * <p><b>推荐（按实例）</b>：{@code autumn.backup.exclude=true} — 当前 JVM 永远不执行备份产出任务，
 * 适合无状态副本中「只读 / 不承担备份」的 Pod，与其它实例共用同一套 YAML 时在 Deployment 或环境变量里单独打开即可。</p>
 * <p><b>可选（集中声明）</b>：</p>
 * <ul>
 *   <li>{@code autumn.backup.exclude-hosts} — 逗号分隔主机名模式；支持 {@code app-*}、{@code *-worker}、{@code *mid*}</li>
 *   <li>{@code autumn.backup.exclude-addresses} — 逗号分隔 IP，与 {@link InetAddress#getHostAddress()} 精确相等即命中</li>
 * </ul>
 * <p>主机匹配依据：{@code InetAddress.getLocalHost()} 的 hostName、canonicalHostName、hostAddress，
 * 以及环境变量 {@code HOSTNAME}、{@code COMPUTERNAME}。</p>
 */
@Slf4j
@Component
public class DatabaseBackupExecutionGuard implements InitFactory.Init {
    /**
     * 显式排除本节点；为 true 时不执行备份（优先级最高，无需配置主机/IP 列表）。
     */
    @Value("${autumn.backup.exclude:false}")
    private boolean excludeSelf;

    @Value("${autumn.backup.exclude-hosts:}")
    private String excludeHostsRaw;

    @Value("${autumn.backup.exclude-addresses:}")
    private String excludeAddressesRaw;

    private List<String> excludeHostPatterns = Collections.emptyList();
    private List<String> excludeAddresses = Collections.emptyList();

    private volatile NodeIdentity cachedIdentity;
    private volatile Boolean cachedExcluded;

    public void init() {
        excludeHostPatterns = splitCsv(excludeHostsRaw);
        excludeAddresses = splitCsv(excludeAddressesRaw);
        if (excludeSelf) {
            log.info("Database backup is excluded on this node.");
        } else if (!excludeHostPatterns.isEmpty() || !excludeAddresses.isEmpty()) {
            log.info("Database backup is excluded for ip/host: exclude-hosts={}, exclude-addresses={}", excludeHostPatterns, excludeAddresses);
        }
    }

    /**
     * 当前 JVM 所在节点是否禁止执行备份产出任务。
     */
    public boolean isExcluded() {
        if (excludeSelf) {
            return true;
        }
        if (excludeHostPatterns.isEmpty() && excludeAddresses.isEmpty()) {
            return false;
        }
        if (cachedExcluded != null) {
            return cachedExcluded;
        }
        synchronized (this) {
            if (cachedExcluded != null) {
                return cachedExcluded;
            }
            NodeIdentity id = identity();
            boolean hit = matchesExclude(id);
            cachedExcluded = hit;
            if (hit && log.isDebugEnabled()) {
                log.debug("当前节点命中备份排除: identities={}", id.describe());
            }
            return hit;
        }
    }

    /**
     * 若当前节点允许执行备份则直接返回；否则抛出 {@link IllegalStateException}。
     */
    public void assertBackupAllowed() {
        if (!isExcluded()) {
            return;
        }
        if (excludeSelf) {
            throw new IllegalStateException("当前节点已配置 autumn.backup.exclude=true，不执行数据库备份；请使用未排除的实例发起备份。");
        }
        throw new IllegalStateException("当前节点匹配 autumn.backup.exclude-hosts / exclude-addresses，不执行数据库备份。节点标识：" + identity().describe());
    }

    private NodeIdentity identity() {
        if (cachedIdentity != null) {
            return cachedIdentity;
        }
        synchronized (this) {
            if (cachedIdentity != null) {
                return cachedIdentity;
            }
            cachedIdentity = NodeIdentity.capture();
            return cachedIdentity;
        }
    }

    private boolean matchesExclude(NodeIdentity id) {
        for (String addr : excludeAddresses) {
            if (id.hostAddress != null && addr.equals(id.hostAddress)) {
                return true;
            }
        }
        for (String name : id.names) {
            for (String pattern : excludeHostPatterns) {
                if (matchesHostPattern(name, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 主机名匹配：精确（忽略大小写）、{@code prefix*}、{@code *suffix}、{@code *infix*}（双侧通配）。
     */
    static boolean matchesHostPattern(String identity, String rawPattern) {
        if (StringUtils.isBlank(identity) || rawPattern == null) {
            return false;
        }
        String pattern = rawPattern.trim();
        if (pattern.isEmpty()) {
            return false;
        }
        String id = identity.toLowerCase(Locale.ROOT);
        String p = pattern.toLowerCase(Locale.ROOT);
        boolean starStart = p.startsWith("*");
        boolean starEnd = p.endsWith("*");
        if (!starStart && !starEnd) {
            return id.equals(p);
        }
        if (starStart && starEnd && p.length() >= 2) {
            String mid = p.substring(1, p.length() - 1);
            return !mid.isEmpty() && id.contains(mid);
        }
        if (starEnd && !starStart) {
            String prefix = p.substring(0, p.length() - 1);
            return !prefix.isEmpty() && id.startsWith(prefix);
        }
        if (starStart && !starEnd) {
            String suffix = p.substring(1);
            return !suffix.isEmpty() && id.endsWith(suffix);
        }
        return false;
    }

    private static List<String> splitCsv(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        String[] parts = StringUtils.split(raw, ',');
        List<String> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part != null) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    list.add(t);
                }
            }
        }
        return list.isEmpty() ? Collections.emptyList() : list;
    }

    private static final class NodeIdentity {
        final List<String> names;
        final String hostAddress;

        NodeIdentity(List<String> names, String hostAddress) {
            this.names = names;
            this.hostAddress = hostAddress;
        }

        static NodeIdentity capture() {
            List<String> names = new ArrayList<>(8);
            String ip = null;
            try {
                InetAddress local = InetAddress.getLocalHost();
                if (local != null) {
                    ip = local.getHostAddress();
                    addNonBlank(names, local.getHostName());
                    addNonBlank(names, local.getCanonicalHostName());
                }
            } catch (Exception e) {
                log.debug("解析本机 InetAddress 失败: {}", e.getMessage());
            }
            addNonBlank(names, safeEnv("HOSTNAME"));
            addNonBlank(names, safeEnv("COMPUTERNAME"));
            return new NodeIdentity(names, ip);
        }

        String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append("names=").append(names);
            if (hostAddress != null) {
                sb.append(", address=").append(hostAddress);
            }
            return sb.toString();
        }
    }

    private static void addNonBlank(List<String> list, String s) {
        if (StringUtils.isNotBlank(s)) {
            String t = s.trim();
            if (!list.contains(t)) {
                list.add(t);
            }
        }
    }

    private static String safeEnv(String key) {
        try {
            String v = System.getenv(key);
            return v != null ? v.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
