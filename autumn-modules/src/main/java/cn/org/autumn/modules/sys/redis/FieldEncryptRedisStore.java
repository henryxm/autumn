package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.service.RedisListenerService;
import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 集群模式下字段加密 Redis 存储。
 * <ul>
 *   <li>启动：{@link #syncBootstrapFromEnvironment()} 用 yml/环境变量覆盖 Redis（避免陈旧
 *       {@code write-enabled=false} 挡住新配置）</li>
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

    public boolean isActive() {
        return redisUtils.isOpen() && stringRedisTemplate != null;
    }

    public String refreshChannel() {
        return RedisKeys.getFieldEncryptRefreshChannel(sysConfigService.getNameSpace());
    }

    /**
     * 启动时用当前 yml/环境变量覆盖 Redis 中的写入开关与密钥。
     * 解决：本地已改 {@code crypto.field.enabled=true}，但 Redis 仍残留 {@code write-enabled=false}。
     * 运行中后台切换仍写 Redis；下次进程启动会再次与环境配置对齐。
     */
    public void syncBootstrapFromEnvironment() {
        if (!isActive()) {
            return;
        }
        FieldEncryptConfigSource.Resolved resolved = configSource.resolveFromEnvironment();
        boolean fromEnv = resolved.isConfigWriteEnabled();
        Boolean previous = parseBoolean(readRaw(WRITE_ENABLED));
        write(WRITE_ENABLED, Boolean.toString(fromEnv));
        if (previous != null && !previous.equals(fromEnv)) {
            log.info("Field encrypt Redis write-enabled reconciled: {} -> {} (from env/yml)", previous, fromEnv);
        }

        String envKey = configSource.validKeyBase64(resolved.getKeyBase64());
        if (envKey != null) {
            String cached = trimToNull(readRaw(KEY));
            if (!envKey.equals(cached)) {
                write(KEY, envKey);
                if (cached != null) {
                    log.info("Field encrypt Redis key reconciled from environment");
                }
            }
        }
        String envHash = configSource.validHashBase64(
                StringUtils.isNotBlank(resolved.getHashKeyBase64()) ? resolved.getHashKeyBase64() : envKey);
        if (envHash != null) {
            String cachedHash = trimToNull(readRaw(HASH_KEY));
            if (!envHash.equals(cachedHash)) {
                write(HASH_KEY, envHash);
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
