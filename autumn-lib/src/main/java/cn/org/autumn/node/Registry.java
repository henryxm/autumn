package cn.org.autumn.node;

import cn.org.autumn.config.Config;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 集群节点登记：Redis Hash 上报本机画像；支持远程指派 roles（Pub/Sub 命令）。
 * <p>
 * 默认：未显式配置 {@code autumn.node.registry} 时跟随 {@code autumn.redis.open}；
 * 显式 {@code true}/{@code false} 可覆盖。心跳由 modules 侧 LoopJob 调用 {@link #beat()}。
 * <p>
 * 实际生效还需 Redisson 可用：配置要求开启但 {@code autumn.redis.open=false} 或无 {@code RedissonClient} 时
 * 登记保持未启用（不阻断启动），并打一次 info 提示。
 * <p>
 * 成员活性：每条 field 的 JSON 含 {@code beat}（epoch ms）；{@link #online()} 过滤超时节点并清理，
 * 避免整表 TTL 被存活节点续期导致宕机 uuid 永久残留（SEQUENTIAL 令牌卡死）。
 */
@Slf4j
@Component
public class Registry implements LoadFactory.Must {

    public static final String REGISTRY_KEY = "autumn.node.registry";
    public static final String NS_KEY = "autumn.node.namespace";
    /** 与 Registry 联动的 Redis 总开关（未显式配 registry 时跟随此值）。 */
    public static final String REDIS_OPEN_KEY = "autumn.redis.open";
    /** 成员无心跳超过此时长视为离线（毫秒）；与 beat 写入的整表安全网 TTL 对齐。 */
    public static final long STALE_MS = 180_000L;
    private static final long MAP_TTL_SECONDS = 180L;

    private final ObjectProvider<RedissonClient> redissonProvider;
    private final ProfileService profileService;
    /** 配置意图且 Redis/Redisson 可用时为 true。 */
    private volatile boolean enabled;
    private volatile boolean subscribed;
    /** 避免「想开但不可用」刷屏。 */
    private volatile boolean unavailableLogged;

    public Registry(ObjectProvider<RedissonClient> redissonProvider, ProfileService profileService) {
        this.redissonProvider = redissonProvider;
        this.profileService = profileService;
    }

    @Override
    @Order(20)
    public void must() {
        refreshEnabled();
        if (!enabled) {
            return;
        }
        try {
            profileService.ensure();
            beat();
            subscribeCmd();
        } catch (Exception e) {
            log.info("Node registry start skipped: {}", brief(e));
        }
    }

    /**
     * 刷新生效状态：配置要求开启 + Redisson 可用才为 true；否则 false 且不抛错。
     */
    public void refreshEnabled() {
        boolean want = isEnabledConfig();
        if (!want) {
            enabled = false;
            return;
        }
        if (!redisOpen()) {
            enabled = false;
            logUnavailable("autumn.redis.open=false");
            return;
        }
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            enabled = false;
            logUnavailable("RedissonClient unavailable");
            return;
        }
        enabled = true;
    }

    /** 配置意图开启（未考虑 Redis 是否可用）。 */
    public boolean configured() {
        return isEnabledConfig();
    }

    /** 实际可用：配置开启且 Redisson 就绪。 */
    public boolean enabled() {
        refreshEnabled();
        return enabled;
    }

    private void logUnavailable(String reason) {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        log.info("Node registry inactive ({}): need autumn.redis.open=true and Redisson; startup continues", reason);
    }

    private static String brief(Exception e) {
        return e == null ? "unknown" : e.toString();
    }

    /** 在线节点 uuid 列表（按字典序）；已按 {@link #STALE_MS} 过滤陈旧心跳。 */
    public List<String> online() {
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            return List.of();
        }
        try {
            RMap<String, String> map = client.getMap(nodesKey());
            long now = System.currentTimeMillis();
            List<String> ids = new ArrayList<>();
            List<String> stale = new ArrayList<>();
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (isFreshEntry(e.getValue(), now, STALE_MS)) {
                    ids.add(e.getKey());
                } else {
                    stale.add(e.getKey());
                }
            }
            for (String id : stale) {
                map.fastRemove(id);
            }
            Collections.sort(ids);
            return ids;
        } catch (Exception e) {
            log.debug("registry online failed: {}", e.toString());
            return List.of();
        }
    }

    public Map<String, String> snapshot() {
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            return Map.of();
        }
        try {
            return client.getMap(nodesKey());
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 结构化成员列表：uuid / beat / online / roles / labels / version / self。
     * online 按 {@link #STALE_MS} 判定；不在此清理陈旧 field（清理仍由 {@link #online()}）。
     */
    public List<Map<String, Object>> members() {
        Map<String, String> raw = snapshot();
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        String self = null;
        try {
            self = profileService.peekUuid();
        } catch (Exception ignored) {
            // ignore
        }
        if (StringUtils.isBlank(self)) {
            try {
                self = profileService.uuid();
            } catch (Exception ignored) {
                // ignore
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            Map<String, Object> row = parseMember(e.getKey(), e.getValue(), now, self);
            if (row != null) {
                list.add(row);
            }
        }
        list.sort((a, b) -> String.valueOf(a.get("uuid")).compareTo(String.valueOf(b.get("uuid"))));
        return list;
    }

    static Map<String, Object> parseMember(String key, String json, long nowMs, String selfUuid) {
        Map<String, Object> row = new LinkedHashMap<>();
        String uuid = StringUtils.isNotBlank(key) ? key.trim() : "";
        long beat = 0L;
        List<String> roles = List.of();
        Map<String, String> labels = Map.of();
        int version = 0;
        if (StringUtils.isNotBlank(json)) {
            try {
                JSONObject o = JSON.parseObject(json);
                if (o != null) {
                    if (StringUtils.isNotBlank(o.getString("uuid"))) {
                        uuid = o.getString("uuid").trim();
                    }
                    beat = o.getLongValue("beat");
                    JSONObject profile = o.getJSONObject("profile");
                    if (profile != null) {
                        if (StringUtils.isBlank(uuid) && StringUtils.isNotBlank(profile.getString("uuid"))) {
                            uuid = profile.getString("uuid").trim();
                        }
                        version = profile.getIntValue("version");
                        List<String> r = profile.getList("roles", String.class);
                        if (r != null) {
                            roles = List.copyOf(r);
                        }
                        JSONObject lab = profile.getJSONObject("labels");
                        if (lab != null && !lab.isEmpty()) {
                            Map<String, String> lm = new LinkedHashMap<>();
                            for (String k : lab.keySet()) {
                                Object v = lab.get(k);
                                if (v != null) {
                                    lm.put(k, String.valueOf(v));
                                }
                            }
                            labels = lm;
                        }
                    }
                }
            } catch (Exception ignored) {
                // keep defaults
            }
        }
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        row.put("uuid", uuid);
        row.put("beat", beat);
        row.put("online", isFreshEntry(json, nowMs, STALE_MS));
        row.put("roles", roles);
        row.put("labels", labels);
        row.put("version", version);
        row.put("self", StringUtils.isNotBlank(selfUuid) && Uuid.equals(uuid, selfUuid));
        return row;
    }

    /**
     * 向目标节点下发 roles 指派（异步；目标机监听后调用 {@link ProfileService#roles}）。
     * 若目标为本机，则同步落盘并立即 {@link #beat()}，避免仅依赖本进程 Pub/Sub 回环。
     */
    public boolean assign(String targetUuid, List<String> roles) {
        if (StringUtils.isBlank(targetUuid)) {
            return false;
        }
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            return false;
        }
        String target = targetUuid.trim();
        List<String> nextRoles = roles != null ? roles : List.of();
        JSONObject cmd = new JSONObject();
        cmd.put("uuid", target);
        cmd.put("roles", nextRoles);
        cmd.put("from", profileService.uuid());
        try {
            RTopic topic = client.getTopic(cmdChannel());
            topic.publish(cmd.toJSONString());
        } catch (Exception e) {
            log.warn("registry assign publish failed: {}", e.toString());
            return false;
        }
        try {
            if (Uuid.equals(target, profileService.uuid())) {
                profileService.roles(nextRoles);
                beat();
            }
        } catch (Exception e) {
            log.warn("registry assign self-apply failed: {}", e.toString());
        }
        return true;
    }

    public void beat() {
        if (!enabled()) {
            return;
        }
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            logUnavailable("RedissonClient unavailable");
            return;
        }
        try {
            Profile p = profileService.ensure();
            RMap<String, String> map = client.getMap(nodesKey());
            JSONObject entry = new JSONObject();
            entry.put("uuid", p.getUuid());
            entry.put("beat", System.currentTimeMillis());
            entry.put("profile", p);
            map.put(p.getUuid(), entry.toJSONString());
            map.expire(MAP_TTL_SECONDS, TimeUnit.SECONDS);
            subscribeCmd();
        } catch (Exception e) {
            log.debug("registry beat failed: {}", e.toString());
        }
    }

    private synchronized void subscribeCmd() {
        if (subscribed) {
            return;
        }
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            logUnavailable("RedissonClient unavailable");
            return;
        }
        try {
            RTopic topic = client.getTopic(cmdChannel());
            topic.addListener(String.class, (channel, msg) -> handleCmd(msg));
            subscribed = true;
        } catch (Exception e) {
            subscribed = false;
            log.info("Node registry subscribe skipped: {}", brief(e));
        }
    }

    private void handleCmd(String msg) {
        try {
            JSONObject cmd = JSON.parseObject(msg);
            if (cmd == null) {
                return;
            }
            String target = cmd.getString("uuid");
            String self = profileService.uuid();
            if (!Uuid.equals(target, self)) {
                return;
            }
            List<String> roles = cmd.getList("roles", String.class);
            profileService.roles(roles != null ? roles : List.of());
            beat();
            if (log.isInfoEnabled()) {
                log.info("Applied remote profile roles uuid={} roles={}", self, roles);
            }
        } catch (Exception e) {
            log.warn("registry cmd handle failed: {}", e.toString());
        }
    }

    /** 命名空间（Redis 键/频道后缀）；供管理页展示。 */
    public String namespacePublic() {
        return namespace();
    }

    /** 当前是否跟随 Redis 开关（未显式写 {@code autumn.node.registry}）。 */
    public boolean followsRedisOpen() {
        return StringUtils.isBlank(Config.getEnv(REGISTRY_KEY));
    }

    /** Redis 总开关是否开启（供管理页提示）。 */
    public boolean redisOpen() {
        return isTruthy(Config.getEnv(REDIS_OPEN_KEY));
    }

    String namespace() {
        String ns = Config.getEnv(NS_KEY);
        if (StringUtils.isBlank(ns)) {
            ns = Config.getEnv("CLUSTER_NAMESPACE");
        }
        if (StringUtils.isBlank(ns)) {
            ns = "default";
        }
        return ns.trim();
    }

    private String nodesKey() {
        return "autumn:cluster:nodes:" + namespace();
    }

    private String cmdChannel() {
        return "autumn:cluster:profile-cmd:" + namespace();
    }

    /**
     * 登记条目是否新鲜。无 {@code beat} 的旧格式视为陈旧（升级后须重新心跳）。
     */
    static boolean isFreshEntry(String json, long nowMs, long staleMs) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            JSONObject o = JSON.parseObject(json);
            if (o == null || !o.containsKey("beat")) {
                return false;
            }
            long beat = o.getLongValue("beat");
            return beat > 0L && (nowMs - beat) <= staleMs;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否启用集群登记：
     * <ul>
     *   <li>显式 {@code autumn.node.registry=true/false}（或 1/0）→ 以显式为准</li>
     *   <li>未配置 → 跟随 {@code autumn.redis.open}</li>
     * </ul>
     */
    static boolean isEnabledConfig() {
        String explicit = Config.getEnv(REGISTRY_KEY);
        if (StringUtils.isNotBlank(explicit)) {
            return isTruthy(explicit);
        }
        return isTruthy(Config.getEnv(REDIS_OPEN_KEY));
    }

    static boolean isTruthy(String raw) {
        String v = StringUtils.trimToEmpty(raw);
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
    }
}
