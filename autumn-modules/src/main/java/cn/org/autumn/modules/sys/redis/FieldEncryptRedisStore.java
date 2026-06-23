package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.service.RedisListenerService;
import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 集群模式下字段加密 Redis 存储：读优先；无值时从环境变量回填并写入 Redis。
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

    /** 读取主密钥 Base64；Redis 无值时用环境变量回填。 */
    public String readKeyBase64() {
        String cached = trimToNull(readRaw(KEY));
        if (cached != null) {
            return cached;
        }
        String fromEnv = configSource.validKeyBase64();
        if (fromEnv == null) {
            return null;
        }
        write(KEY, fromEnv);
        return fromEnv;
    }

    /** 读取盲索引密钥 Base64；Redis 无值时用环境变量回填。 */
    public String readHashKeyBase64() {
        String cached = trimToNull(readRaw(HASH_KEY));
        if (cached != null) {
            return cached;
        }
        String fromEnv = configSource.validHashKeyBase64(readKeyBase64());
        if (fromEnv == null) {
            return null;
        }
        write(HASH_KEY, fromEnv);
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
            log.warn("字段加密集群刷新通知发布失败:{}", e.getMessage());
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
        log.warn("字段加密 Redis 写入开关值无效:{}，将使用环境变量默认值", raw);
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
