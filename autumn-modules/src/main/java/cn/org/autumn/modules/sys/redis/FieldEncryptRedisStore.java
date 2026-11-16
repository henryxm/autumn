package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.service.RedisListenerService;
import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 集群模式下字段加密 Redis 存储。
 * <ul>
 *   <li>启动：{@link #syncBootstrapFromEnvironment()} 对齐密钥；写开关见该方法注释（保留一键开启）</li>
 *   <li>运行中：读优先 Redis；无值时从环境变量回填</li>
 * </ul>
 */
@Slf4j
@Component
public class FieldEncryptRedisStore {

    static final String WRITE_ENABLED = "write-enabled";
    static final String KEY = "key";
    static final String HASH_KEY = "hash-key";

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private FieldEncryptConfigSource configSource;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private RedisListenerService redisListenerService;

    @Autowired(required = false)
    @Lazy
    private EnvBean envBean;

    @Autowired
    private Environment environment;

    public boolean isActive() {
        return redisUtils.isOpen() && stringRedisTemplate != null;
    }

    public String refreshChannel() {
        return RedisKeys.getFieldEncryptRefreshChannel(sysConfigService.getNameSpace());
    }

    /**
     * 启动对齐 Redis 密钥，并按策略处理写开关：
     * <ul>
     *   <li>Redis 无写开关 → 用 env/yml {@code enabled} 种子</li>
     *   <li>env {@code enabled=true} → 可强制推高 Redis（升级打开写加密）</li>
     *   <li>env {@code enabled=false} 且 Redis 已为 true → <strong>不覆盖</strong>（保留 fieldencrypt 一键开启，避免滚动重启关写）</li>
     *   <li>合法 key/hash-key 仍从 env 对齐到 Redis，保证新节点带上密钥</li>
     * </ul>
     */
    public void syncBootstrapFromEnvironment() {
        if (!isActive()) {
            return;
        }
        FieldEncryptConfigSource.Resolved resolved = configSource.resolveFromEnvironment();
        boolean fromEnv = resolved.isConfigWriteEnabled();
        Boolean previous = parseBoolean(readRaw(WRITE_ENABLED));
        if (previous == null) {
            write(WRITE_ENABLED, Boolean.toString(fromEnv));
            if (log.isDebugEnabled()) {
                log.debug("Field encrypt Redis write-enabled seeded={} from env/yml", fromEnv);
            }
        } else if (fromEnv && !previous) {
            write(WRITE_ENABLED, "true");
            log.info("Field encrypt Redis write-enabled reconciled: false -> true (from env/yml force-on)");
        } else if (!fromEnv && previous) {
            log.info("Field encrypt Redis write-enabled kept=true (env enabled=false does not override runtime one-click)");
        }

        // 密钥：仅在 Redis 无 key 时用 env 种子；已有不同 key 绝不覆盖（保护历史 ENC$）
        String envKey = configSource.validKeyBase64(resolved.getKeyBase64());
        if (envKey != null) {
            String cached = configSource.validKeyBase64(trimToNull(readRaw(KEY)));
            if (cached == null) {
                write(KEY, envKey);
                if (log.isDebugEnabled()) {
                    log.debug("Field encrypt Redis key seeded from environment");
                }
            } else if (!envKey.equals(cached)) {
                log.warn("Field encrypt Redis key kept (env/yml key differs); refuse overwrite to protect historical ENC$ ciphertext");
            }
        }
        String envHash = configSource.validHashBase64(
                StringUtils.isNotBlank(resolved.getHashKeyBase64()) ? resolved.getHashKeyBase64() : envKey);
        if (envHash != null) {
            String cachedHash = configSource.validHashBase64(trimToNull(readRaw(HASH_KEY)));
            if (cachedHash == null) {
                write(HASH_KEY, envHash);
            } else if (!envHash.equals(cachedHash)) {
                log.warn("Field encrypt Redis hash-key kept (env/yml differs); refuse overwrite");
            }
        }
    }

    /** 读取写入开关；Redis 无值时用 yml/环境变量默认值回填。 */
    public Boolean readWriteEnabled() {
        Boolean parsed = parseBoolean(readRaw(WRITE_ENABLED));
        if (parsed != null) {
            return parsed;
        }
        boolean fromEnv = configSource.resolveFromEnvironment().isConfigWriteEnabled();
        write(WRITE_ENABLED, Boolean.toString(fromEnv));
        return fromEnv;
    }

    public void writeWriteEnabled(boolean enabled) {
        write(WRITE_ENABLED, Boolean.toString(enabled));
    }

    /**
     * Spring 早期（Shiro Cookie Bean 创建前）：用 env/node namespace 读 Redis 密钥注入内存，
     * 避免走 {@link SysConfigService#getNameSpace()}（此时密文配置可能尚无法解密）。
     *
     * @return 是否已使 {@link FieldEncryptService#isKeyConfigured()} 为 true
     */
    public boolean tryHydrateKeysEarly(FieldEncryptService fieldEncryptService) {
        if (fieldEncryptService == null || fieldEncryptService.isKeyConfigured()) {
            return fieldEncryptService != null && fieldEncryptService.isKeyConfigured();
        }
        if (!isActive()) {
            return false;
        }
        try {
            String keyBase64 = null;
            String hashBase64 = null;
            String hitNs = null;
            for (String ns : earlyNamespaces()) {
                keyBase64 = configSource.validKeyBase64(trimToNull(
                        stringRedisTemplate.opsForValue().get(RedisKeys.getFieldEncryptKey(ns, KEY))));
                if (keyBase64 != null) {
                    hitNs = ns;
                    hashBase64 = configSource.validHashBase64(trimToNull(
                            stringRedisTemplate.opsForValue().get(RedisKeys.getFieldEncryptKey(ns, HASH_KEY))));
                    if (hashBase64 == null) {
                        hashBase64 = keyBase64;
                    }
                    break;
                }
            }
            if (keyBase64 == null) {
                return false;
            }
            fieldEncryptService.applyExternalKeys(keyBase64, hashBase64, FieldEncryptService.SOURCE_REDIS);
            Boolean writeEnabled = parseBoolean(trimToNull(
                    stringRedisTemplate.opsForValue().get(RedisKeys.getFieldEncryptKey(hitNs, WRITE_ENABLED))));
            // 写开关读失败不阻断密钥；完整对齐仍由 InitFactory 完成
            if (writeEnabled != null) {
                fieldEncryptService.setRuntimeWriteEncryptOverride(writeEnabled);
                fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_REDIS);
            }
            return fieldEncryptService.isKeyConfigured();
        } catch (Exception e) {
            log.warn("字段加密 Redis 密钥早期加载失败（将在 Init 重试）msg={} | 处理：确认 Redis 已认证连通，或在 yml 配置 autumn.crypto.field.key；见 docs/AI_FIELD_ENCRYPT.md §3.2",
                    e.getMessage());
            return false;
        }
    }

    private String[] earlyNamespaces() {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        String fromEnvBean = null;
        try {
            if (envBean != null) {
                fromEnvBean = StringUtils.trimToNull(envBean.getClusterNamespace());
            }
        } catch (Exception ignored) {
        }
        if (fromEnvBean != null) {
            set.add(fromEnvBean);
        }
        String fromProp = StringUtils.trimToNull(environment.getProperty("autumn.node.namespace"));
        if (fromProp != null) {
            set.add(fromProp);
        }
        set.add("default");
        return set.toArray(new String[0]);
    }

    /** 读取主密钥 Base64；Redis 无值或无效时用环境变量回填。 */
    public String readKeyBase64() {
        String cached = configSource.validKeyBase64(trimToNull(readRaw(KEY)));
        if (cached != null) {
            return cached;
        }
        String fromEnv = configSource.validKeyBase64();
        if (fromEnv == null) {
            return null;
        }
        write(KEY, fromEnv);
        publishRefresh();
        return fromEnv;
    }

    /** 读取盲索引密钥 Base64；Redis 无值或无效时用环境变量回填。 */
    public String readHashKeyBase64() {
        String cached = configSource.validHashBase64(trimToNull(readRaw(HASH_KEY)));
        if (cached != null) {
            return cached;
        }
        String fromEnv = configSource.validHashKeyBase64(readKeyBase64());
        if (fromEnv == null) {
            return null;
        }
        write(HASH_KEY, fromEnv);
        publishRefresh();
        return fromEnv;
    }

    public void publishRefresh() {
        if (!isActive()) {
            return;
        }
        String channel = refreshChannel();
        try {
            if (redisListenerService != null && redisListenerService.publish(channel, "reload")) {
                return;
            }
            stringRedisTemplate.convertAndSend(channel, "reload");
        } catch (Exception e) {
            log.warn("Field encryption cluster refresh notification publish failed:{}", e.getMessage());
        }
    }

    public boolean subscribeRefresh(Runnable onRefresh) {
        if (!isActive() || onRefresh == null || redisListenerService == null) {
            return false;
        }
        MessageHandler handler = (channel, messageBody) -> onRefresh.run();
        return redisListenerService.subscribe(refreshChannel(), handler);
    }

    private Boolean parseBoolean(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String v = raw.trim();
        if ("true".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        }
        log.warn("Invalid field encryption Redis write switch value:{}, using environment default", raw);
        return null;
    }

    private String readRaw(String suffix) {
        if (!isActive()) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(redisKey(suffix));
    }

    private void write(String suffix, String value) {
        if (!isActive()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(redisKey(suffix), value);
    }

    private String redisKey(String suffix) {
        return RedisKeys.getFieldEncryptKey(sysConfigService.getNameSpace(), suffix);
    }

    private static String trimToNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
