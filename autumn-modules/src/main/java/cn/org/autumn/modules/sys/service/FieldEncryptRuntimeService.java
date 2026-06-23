package cn.org.autumn.modules.sys.service;

import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.redis.FieldEncryptRedisStore;
import cn.org.autumn.site.InitFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字段存储加密运行时协调。
 * <ul>
 *   <li>集群（Redis 开启）：开关与密钥从 {@link FieldEncryptRedisStore} 加载</li>
 *   <li>单机：密钥来自环境变量，写入开关可覆盖到 sys_config</li>
 * </ul>
 */
@Slf4j
@Service
public class FieldEncryptRuntimeService implements InitFactory.Init, InitFactory.After {

    static final String RUNTIME_WRITE_ENABLED_KEY = "field_encrypt_runtime_write_enabled";

    private static final int SUBSCRIBE_MAX_ATTEMPTS = 12;
    private static final long SUBSCRIBE_RETRY_MS = 2000L;

    @Autowired
    private FieldEncryptService fieldEncryptService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private FieldEncryptRedisStore fieldEncryptRedisStore;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    private volatile boolean refreshSubscribed;

    @Override
    public void init() {
        if (isClusterMode()) {
            syncFromRedis();
        } else {
            loadStandaloneWriteSwitch();
        }
        fieldEncryptService.validateAfterBootstrap();
    }

    @Override
    public void after() {
        if (isClusterMode()) {
            scheduleSubscribeRefresh(0);
        }
    }

    public boolean isClusterMode() {
        return fieldEncryptRedisStore.isActive();
    }

    /** 从 Redis 同步到内存（读方法会在 Redis 无值时用环境变量回填）。 */
    public void reloadFromRedis() {
        if (isClusterMode()) {
            syncFromRedis();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setWriteEncryptEnabled(boolean enabled) {
        if (enabled && !fieldEncryptService.isKeyConfigured() && isClusterMode()) {
            syncFromRedis();
        }
        assertWriteReady(enabled);
        if (isClusterMode()) {
            fieldEncryptRedisStore.writeWriteEnabled(enabled);
            fieldEncryptService.setRuntimeWriteEncryptOverride(enabled);
            fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_REDIS);
            fieldEncryptRedisStore.publishRefresh();
            return;
        }
        fieldEncryptService.setRuntimeWriteEncryptOverride(enabled);
        fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_SYS_CONFIG);
        upsertSysConfigValue(RUNTIME_WRITE_ENABLED_KEY, Boolean.toString(enabled));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetWriteEncryptOverride() {
        if (isClusterMode()) {
            boolean defaultEnabled = fieldEncryptService.isConfigWriteEncryptEnabled();
            fieldEncryptRedisStore.writeWriteEnabled(defaultEnabled);
            fieldEncryptService.setRuntimeWriteEncryptOverride(defaultEnabled);
            fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_REDIS);
            fieldEncryptRedisStore.publishRefresh();
            return;
        }
        fieldEncryptService.setRuntimeWriteEncryptOverride(null);
        fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_CONFIG);
        deleteSysConfigKey(RUNTIME_WRITE_ENABLED_KEY);
    }

    private void syncFromRedis() {
        String keyBase64 = fieldEncryptRedisStore.readKeyBase64();
        String hashKeyBase64 = fieldEncryptRedisStore.readHashKeyBase64();
        if (StringUtils.isNotBlank(keyBase64)) {
            fieldEncryptService.applyExternalKeys(keyBase64, hashKeyBase64, FieldEncryptService.SOURCE_REDIS);
        } else {
            fieldEncryptService.reloadKeysFromEnvironment();
        }
        fieldEncryptService.setRuntimeWriteEncryptOverride(fieldEncryptRedisStore.readWriteEnabled());
        fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_REDIS);
    }

    private void assertWriteReady(boolean enabled) {
        if (!enabled) {
            return;
        }
        if (!fieldEncryptService.isKeyConfigured()) {
            throw new IllegalStateException("须先配置有效密钥后才能开启写入加密（集群请在 Redis 或环境变量中配置 key）");
        }
        if (!fieldEncryptService.isHashKeyConfigured()) {
            throw new IllegalStateException("须先配置有效的 hash-key 后才能开启写入加密");
        }
    }

    private void loadStandaloneWriteSwitch() {
        String value = sysConfigService.getValue(RUNTIME_WRITE_ENABLED_KEY);
        if (StringUtils.isBlank(value)) {
            fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_CONFIG);
            return;
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            fieldEncryptService.setRuntimeWriteEncryptOverride(true);
            fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_SYS_CONFIG);
            return;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            fieldEncryptService.setRuntimeWriteEncryptOverride(false);
            fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_SYS_CONFIG);
            return;
        }
        log.warn("sys_config {} 值无效:{}，忽略运行时写入覆盖", RUNTIME_WRITE_ENABLED_KEY, value);
        fieldEncryptService.setWriteSwitchSource(FieldEncryptService.SOURCE_CONFIG);
    }

    private void scheduleSubscribeRefresh(int attempt) {
        if (refreshSubscribed || !isClusterMode()) {
            return;
        }
        if (fieldEncryptRedisStore.subscribeRefresh(this::syncFromRedis)) {
            refreshSubscribed = true;
            if (log.isInfoEnabled()) {
                log.info("字段加密集群已订阅 Redis 刷新频道");
            }
            return;
        }
        if (attempt >= SUBSCRIBE_MAX_ATTEMPTS) {
            log.warn("字段加密集群 Redis 刷新订阅失败，已达最大重试次数");
            return;
        }
        asyncTaskExecutor.execute(() -> {
            try {
                Thread.sleep(SUBSCRIBE_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            scheduleSubscribeRefresh(attempt + 1);
        });
    }

    private void upsertSysConfigValue(String key, String value) {
        if (sysConfigService.hasKey(key)) {
            sysConfigService.updateValueByKey(key, value);
            return;
        }
        SysConfigEntity config = new SysConfigEntity();
        config.setParamKey(key);
        config.setParamValue(value);
        config.setStatus(1);
        config.setRemark("字段存储加密运行时写入开关（单机）");
        sysConfigService.save(config);
    }

    private void deleteSysConfigKey(String key) {
        if (!sysConfigService.hasKey(key)) {
            return;
        }
        SysConfigEntity entity = sysConfigService.getByKey(key);
        if (entity != null && entity.getId() != null) {
            sysConfigService.deleteBatch(new Long[]{entity.getId()});
        }
    }
}
