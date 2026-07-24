package cn.org.autumn.node;

import cn.org.autumn.config.Config;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
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
 * 默认不开启；配置 {@code autumn.node.registry=true} 后启动。心跳由 modules 侧 LoopJob 调用 {@link #beat()}。
 * <p>
 * 成员活性：每条 field 的 JSON 含 {@code beat}（epoch ms）；{@link #online()} 过滤超时节点并清理，
 * 避免整表 TTL 被存活节点续期导致宕机 uuid 永久残留（SEQUENTIAL 令牌卡死）。
 */
@Slf4j
@Component
public class Registry implements LoadFactory.Must {

    public static final String REGISTRY_KEY = "autumn.node.registry";
    public static final String NS_KEY = "autumn.node.namespace";
    /** 成员无心跳超过此时长视为离线（毫秒）；与 beat 写入的整表安全网 TTL 对齐。 */
    public static final long STALE_MS = 180_000L;
    private static final long MAP_TTL_SECONDS = 180L;

    private final ObjectProvider<RedissonClient> redissonProvider;
    private final ProfileService profileService;
    private volatile boolean enabled;
    private volatile boolean subscribed;

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
        profileService.ensure();
        beat();
        subscribeCmd();
    }

    public void refreshEnabled() {
        enabled = isEnabledConfig();
    }

    public boolean enabled() {
        refreshEnabled();
        return enabled;
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
     * 向目标节点下发 roles 指派（异步；目标机监听后调用 {@link ProfileService#roles}）。
     */
    public boolean assign(String targetUuid, List<String> roles) {
        if (StringUtils.isBlank(targetUuid)) {
            return false;
        }
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            return false;
        }
        JSONObject cmd = new JSONObject();
        cmd.put("uuid", targetUuid.trim());
        cmd.put("roles", roles != null ? roles : List.of());
        cmd.put("from", profileService.uuid());
        try {
            RTopic topic = client.getTopic(cmdChannel());
            topic.publish(cmd.toJSONString());
            return true;
        } catch (Exception e) {
            log.warn("registry assign publish failed: {}", e.toString());
            return false;
        }
    }

    public void beat() {
        if (!enabled()) {
            return;
        }
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
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
            return;
        }
        try {
            RTopic topic = client.getTopic(cmdChannel());
            topic.addListener(String.class, (channel, msg) -> handleCmd(msg));
            subscribed = true;
        } catch (Exception e) {
            subscribed = false;
            log.debug("registry subscribe failed: {}", e.toString());
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

    private static boolean isEnabledConfig() {
        String v = Config.getEnv(REGISTRY_KEY);
        return "true".equalsIgnoreCase(StringUtils.trimToEmpty(v)) || "1".equals(StringUtils.trimToEmpty(v));
    }
}
