package cn.org.autumn.crypto;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.table.service.MysqlTableService;
import cn.org.autumn.table.utils.HumpConvert;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段存储加解密核心：配置、实体元数据、读写变换与运维辅助。
 * <p>
 * <strong>写入</strong>是否加密由运行时开关 {@link #isWriteEncryptEnabled()} 决定；
 * <strong>读取</strong>解密仅依赖 {@code key} 是否有效（与写入开关无关）。
 * <p>
 * Service 层入口：{@link #onWrite(Object)} / {@link #onRead(Object)}（见 {@code EncryptModuleService}）。
 * 易混概念（vector / searchable / hash 列 / 开关）见 {@code docs/AI_FIELD_ENCRYPT.md} §0。
 */
@Slf4j
@Service
public class FieldEncryptService {

    /**
     * 配置来源：环境变量 / yml
     */
    public static final String SOURCE_ENV = "env";
    /**
     * 配置来源：Redis 集群
     */
    public static final String SOURCE_REDIS = "redis";
    /**
     * 配置来源：yml 默认写入开关
     */
    public static final String SOURCE_CONFIG = "config";
    /**
     * 配置来源：sys_config 运行时覆盖
     */
    public static final String SOURCE_SYS_CONFIG = "sys_config";

    @Autowired
    private FieldEncryptConfigSource configSource;

    @Autowired
    MysqlTableService mysqlTableService;

    private static volatile FieldEncryptService instance;

    private final Map<Class<?>, List<FieldMeta>> entityFields = new ConcurrentHashMap<>();

    private volatile byte[] encryptKey;
    private volatile byte[] hashKey;
    @Getter
    private volatile String prefix = "ENC$v1$";
    @Getter
    private volatile boolean configWriteEncryptEnabled;
    @Getter
    private volatile Boolean runtimeWriteEncryptOverride;
    @Getter
    private volatile String keySource = SOURCE_ENV;
    @Getter
    private volatile String hashKeySource = SOURCE_ENV;
    @Getter
    private volatile String writeSwitchSource = SOURCE_CONFIG;

    // --- 生命周期 ----------------------------------------------------------------

    public static FieldEncryptService get() {
        return instance;
    }

    @PostConstruct
    public void init() {
        instance = this;
        applyEnvironmentConfig(configSource.resolveFromEnvironment());
        scanPackage();
        if (log.isDebugEnabled()) {
            if (isConfigWriteEncryptEnabled()) {
                log.debug("字段存储加密配置默认写入:开，密文前缀:{}", prefix);
            } else if (isKeyConfigured()) {
                log.debug("字段存储加密配置默认写入:关，读路径仍可用密钥解密历史密文，前缀:{}", prefix);
            }
        }
    }

    /**
     * 在运行时协调器加载 Redis / sys_config 覆盖之后调用。
     */
    public void validateAfterBootstrap() {
        if (isConfigWriteEncryptEnabled() && !isKeyConfigured()) {
            throw new IllegalStateException("autumn.crypto.field.enabled=true 时须配置有效的 autumn.crypto.field.key（Base64 32 字节），或在集群 Redis 中配置密钥");
        }
        if (isConfigWriteEncryptEnabled() && !isHashKeyConfigured()) {
            throw new IllegalStateException("autumn.crypto.field.hash-key 无效或过短");
        }
        if (isWriteEncryptEnabled() && !isKeyConfigured()) {
            throw new IllegalStateException("字段存储加密写入已开启，但未配置有效密钥");
        }
        if (isWriteEncryptEnabled() && !isHashKeyConfigured()) {
            throw new IllegalStateException("字段存储加密写入已开启，但 hash-key 无效");
        }
        if (log.isDebugEnabled()) {
            log.debug("Field encrypt is ready, write enable:{} secret from:{} gate from:{}", isWriteEncryptEnabled() ? "Yes" : "No", keySource, writeSwitchSource);
        }
    }

    // --- 配置与开关 ----------------------------------------------------------------

    public void setRuntimeWriteEncryptOverride(Boolean enabled) {
        this.runtimeWriteEncryptOverride = enabled;
        if (log.isDebugEnabled()) {
            log.debug("字段存储加密运行时写入开关:{}", enabled == null ? "跟随配置(" + configWriteEncryptEnabled + ")" : enabled);
        }
    }

    public boolean isWriteEncryptEnabled() {
        return runtimeWriteEncryptOverride != null ? runtimeWriteEncryptOverride : configWriteEncryptEnabled;
    }

    public boolean isReadDecryptEnabled() {
        return isKeyConfigured();
    }

    public void setWriteSwitchSource(String source) {
        if (StringUtils.isNotBlank(source)) {
            this.writeSwitchSource = source;
        }
    }

    public boolean isClusterMode() {
        return SOURCE_REDIS.equals(writeSwitchSource);
    }

    public boolean isKeyConfigured() {
        return encryptKey != null && encryptKey.length == 32;
    }

    public boolean isHashKeyConfigured() {
        return hashKey != null && hashKey.length >= 16;
    }

    /**
     * 从 yml/环境变量重新加载密钥到内存。
     */
    public void reloadKeysFromEnvironment() {
        applyEnvironmentKeys(configSource.resolveFromEnvironment());
        keySource = SOURCE_ENV;
        hashKeySource = SOURCE_ENV;
    }

    /**
     * 应用外部来源（如 Redis）的密钥；hash 缺失时与主密钥相同。
     */
    public void applyExternalKeys(String keyBase64, String hashKeyBase64, String source) {
        String validKey = configSource.validKeyBase64(keyBase64);
        if (validKey != null) {
            encryptKey = FieldCrypto.decodeKeyBase64(validKey);
            keySource = source;
        } else if (StringUtils.isNotBlank(keyBase64)) {
            log.warn("字段加密外部密钥无效，忽略 source:{}", source);
        }
        String validHash = configSource.validHashBase64(hashKeyBase64);
        if (validHash != null) {
            hashKey = FieldCrypto.decodeKeyBase64(validHash);
            hashKeySource = source;
        } else if (StringUtils.isNotBlank(hashKeyBase64)) {
            log.warn("字段加密外部 hash-key 无效，忽略 source:{}", source);
        } else if (validKey != null && SOURCE_REDIS.equals(source)) {
            hashKey = encryptKey;
            hashKeySource = source;
        }
    }

    // --- 实体元数据 ----------------------------------------------------------------

    public void registerEntity(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        List<FieldMeta> metas = scanEncryptedFields(clazz);
        if (!metas.isEmpty()) {
            entityFields.put(clazz, Collections.unmodifiableList(metas));
        }
    }

    public void scanPackage() {
        if (mysqlTableService == null) {
            return;
        }
        Set<Class<?>> classes = mysqlTableService.getClasses();
        for (Class<?> clazz : classes) {
            registerEntity(clazz);
        }
    }

    public boolean hasEncryptedFields(Class<?> clazz) {
        return clazz != null && entityFields.containsKey(clazz);
    }

    public List<FieldMeta> getFields(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        return entityFields.getOrDefault(clazz, Collections.emptyList());
    }

    public FieldMeta findByFieldName(Class<?> clazz, String fieldName) {
        for (FieldMeta meta : getFields(clazz)) {
            if (meta.getFieldName().equals(fieldName)) {
                return meta;
            }
        }
        return null;
    }

    // --- 读写变换 ----------------------------------------------------------------

    /**
     * Service 写库前（尊重 {@link FieldEncryptContext#isSkip()}）。
     * 写入开关关时不加密；{@code searchable} 时同步写 hash 列（实体须声明 hash 字段）。
     */
    public void onWrite(Object entity) {
        if (entity != null && !FieldEncryptContext.isSkip()) {
            applyBeforeWrite(entity);
        }
    }

    public void onWrite(List<?> entities) {
        if (entities == null) {
            return;
        }
        for (Object entity : entities) {
            onWrite(entity);
        }
    }

    /** Service / baseMapper 读库后解密；密文内自带 IV，与注解 {@code vector} 无关。 */
    public <E> E onRead(E entity) {
        if (entity != null) {
            applyAfterRead(entity);
        }
        return entity;
    }

    public <E> List<E> onRead(List<E> entities) {
        if (entities != null) {
            for (E entity : entities) {
                applyAfterRead(entity);
            }
        }
        return entities;
    }

    /**
     * 列表查询是否将 searchable 字段改写到 hash 列（{@code BaseService#getCondition}）。
     * 须写入加密开且 hash-key 有效；关写入时 false，回退查明文列，不删 hash 列 DDL。
     */
    public boolean useHashForQuery() {
        return isWriteEncryptEnabled() && isHashKeyConfigured();
    }

    public String hashQueryValue(Class<?> entityClass, String fieldName, String plain) {
        if (!useHashForQuery() || plain == null) {
            return plain;
        }
        FieldMeta meta = findByFieldName(entityClass, fieldName);
        if (meta == null || !meta.isSearchable()) {
            return plain;
        }
        return hashValue(plain);
    }

    public void applyBeforeWrite(Object entity) {
        if (!isWriteEncryptEnabled() || !isKeyConfigured() || entity == null) {
            return;
        }
        transformEntityForPersist(entity, false);
    }

    /**
     * 强制加密落库，不受写入开关影响。
     */
    public void applyEncryptBeforePersist(Object entity) {
        if (!isKeyConfigured() || entity == null) {
            return;
        }
        transformEntityForPersist(entity, true);
    }

    public void applyAfterRead(Object entity) {
        if (!isReadDecryptEnabled() || entity == null) {
            return;
        }
        forFieldValues(entity, (meta, value) -> meta.getField().set(entity, decryptValue(value)));
    }

    /**
     * 还原明文并清空盲索引（配合 {@link FieldEncryptContext#runSkip} 落库）。
     */
    public void applyRestoreBeforeWrite(Object entity) {
        if (!isReadDecryptEnabled() || entity == null) {
            return;
        }
        forFieldValues(entity, (meta, value) -> {
            meta.getField().set(entity, decryptValue(value));
            if (meta.isSearchable() && meta.hasHashField()) {
                meta.getHashField().set(entity, null);
            }
        });
    }

    public String encryptValue(String plain, String vector) {
        if (!isWriteEncryptEnabled() || StringUtils.isBlank(plain)) {
            return plain;
        }
        return encryptValueForced(plain, vector);
    }

    public String encryptValueForced(String plain, String vector) {
        if (!isKeyConfigured() || StringUtils.isBlank(plain)) {
            return plain;
        }
        return FieldCrypto.encrypt(plain, encryptKey, prefix, vector);
    }

    public String decryptValue(String cipher) {
        if (!isReadDecryptEnabled() || StringUtils.isBlank(cipher)) {
            return cipher;
        }
        return FieldCrypto.decrypt(cipher, encryptKey, prefix);
    }

    public String hashValue(String plain) {
        if (!isWriteEncryptEnabled() || StringUtils.isBlank(plain)) {
            return "";
        }
        return hashValueForced(plain);
    }

    public String hashValueForced(String plain) {
        if (!isHashKeyConfigured() || StringUtils.isBlank(plain)) {
            return "";
        }
        return FieldCrypto.hash(plain, hashKey);
    }

    public boolean needsMigration(String value) {
        return isKeyConfigured() && StringUtils.isNotBlank(value) && !value.startsWith(prefix);
    }

    public boolean needsRestore(String value) {
        return isReadDecryptEnabled() && StringUtils.isNotBlank(value) && value.startsWith(prefix);
    }

    /**
     * 列表查询：将 searchable 字段条件改写为盲索引列。
     */
    public Map<String, Object> rewriteSearchCondition(Class<?> entityClass, String fieldName, Object value) {
        Map<String, Object> result = new HashMap<>();
        if (!isWriteEncryptEnabled() || value == null || entityClass == null) {
            return result;
        }
        FieldMeta meta = findByFieldName(entityClass, fieldName);
        if (meta == null || !meta.isSearchable()) {
            return result;
        }
        String plain = value.toString().trim();
        if (plain.isEmpty()) {
            return result;
        }
        result.put(HumpConvert.HumpToUnderline(meta.getHashFieldName()), hashValue(plain));
        return result;
    }

    // --- 运维 / 管理端 ------------------------------------------------------------

    public Map<String, Object> statusSnapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", isWriteEncryptEnabled());
        map.put("writeEncryptEnabled", isWriteEncryptEnabled());
        map.put("configWriteEncryptEnabled", configWriteEncryptEnabled);
        map.put("runtimeWriteEncryptOverride", runtimeWriteEncryptOverride);
        map.put("readDecryptEnabled", isReadDecryptEnabled());
        map.put("prefix", prefix);
        map.put("keyConfigured", isKeyConfigured());
        map.put("hashKeyConfigured", isHashKeyConfigured());
        map.put("keyEditable", false);
        map.put("keySource", keySource);
        map.put("hashKeySource", hashKeySource);
        map.put("writeSwitchSource", writeSwitchSource);
        map.put("clusterMode", isClusterMode());
        map.put("algorithm", "AES-256-GCM");
        map.put("hashAlgorithm", "HMAC-SHA256");
        return map;
    }

    public Map<String, Object> generateDevKeyMaterial() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", FieldCrypto.generateRandomKeyBase64());
        map.put("hashKey", FieldCrypto.generateRandomKeyBase64());
        map.put("hint", "请复制到 autumn.crypto.field.key / hash-key 或环境变量后重启；后台不可修改运行中密钥");
        return map;
    }

    public Map<String, Object> generateDevVector() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vector", FieldCrypto.generateRandomVectorBase64());
        map.put("hint", "12 字节 GCM IV 的 Base64，可用于 @FieldEncrypt(vector) 或加解密测试");
        return map;
    }

    public Map<String, Object> testRoundTrip(String plain, String vector) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!isKeyConfigured()) {
            map.put("error", "未配置有效的 autumn.crypto.field.key，无法执行加解密测试");
            return map;
        }
        String safePlain = plain == null ? "" : plain;
        String safeVector = vector == null ? "" : vector;
        String encrypted = FieldCrypto.encrypt(safePlain, encryptKey, prefix, safeVector);
        String decrypted = FieldCrypto.decrypt(encrypted, encryptKey, prefix);
        map.put("plain", plain);
        map.put("encrypted", encrypted);
        map.put("decrypted", decrypted);
        map.put("hash", isHashKeyConfigured() ? FieldCrypto.hash(safePlain, hashKey) : "");
        map.put("match", StringUtils.equals(plain, decrypted));
        map.put("writeEncryptEnabled", isWriteEncryptEnabled());
        return map;
    }

    /**
     * 管理页：对库内密文字符串解密（仅内存，不写库）。
     */
    public Map<String, Object> testDecrypt(String cipher) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!isKeyConfigured()) {
            map.put("error", "未配置有效的 autumn.crypto.field.key，无法解密");
            return map;
        }
        String safeCipher = cipher == null ? "" : cipher.trim();
        if (safeCipher.isEmpty()) {
            map.put("error", "密文不能为空");
            return map;
        }
        map.put("cipher", cipher);
        if (!safeCipher.startsWith(prefix)) {
            map.put("error", "输入不是有效密文（须以 " + prefix + " 开头）");
            return map;
        }
        try {
            String plain = FieldCrypto.decrypt(safeCipher, encryptKey, prefix);
            map.put("plain", plain);
            map.put("success", true);
        } catch (Exception e) {
            map.put("error", "解密失败，请确认密钥与密文匹配");
            if (log.isDebugEnabled()) {
                log.debug("字段解密测试失败:{}", e.getMessage());
            }
        }
        return map;
    }

    // --- 内部 --------------------------------------------------------------------

    private void applyEnvironmentConfig(FieldEncryptConfigSource.Resolved resolved) {
        configWriteEncryptEnabled = resolved.isConfigWriteEnabled();
        prefix = resolved.getPrefix();
        applyEnvironmentKeys(resolved);
        keySource = SOURCE_ENV;
        hashKeySource = SOURCE_ENV;
    }

    private void applyEnvironmentKeys(FieldEncryptConfigSource.Resolved resolved) {
        encryptKey = FieldCrypto.decodeKeyBase64(resolved.getKeyBase64());
        hashKey = FieldCrypto.decodeKeyBase64(resolved.getHashKeyBase64());
    }

    private void transformEntityForPersist(Object entity, boolean force) {
        List<FieldMeta> metas = entityFields.get(entity.getClass());
        if (metas == null || metas.isEmpty()) {
            return;
        }
        forFieldValues(entity, (meta, plain) -> {
            if (plain.isEmpty()) {
                return;
            }
            String encrypted = force ? encryptValueForced(plain, meta.getVector()) : encryptValue(plain, meta.getVector());
            meta.getField().set(entity, encrypted);
            if (meta.isSearchable() && meta.hasHashField()) {
                meta.getHashField().set(entity, force ? hashValueForced(plain) : hashValue(plain));
            }
        });
    }

    private void forFieldValues(Object entity, FieldValueConsumer consumer) {
        List<FieldMeta> metas = entityFields.get(entity.getClass());
        if (metas == null || metas.isEmpty()) {
            return;
        }
        for (FieldMeta meta : metas) {
            try {
                Object raw = meta.getField().get(entity);
                if (raw instanceof String) {
                    consumer.accept(meta, (String) raw);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("字段加密处理失败:" + meta.getFieldName(), e);
            }
        }
    }

    private List<FieldMeta> scanEncryptedFields(Class<?> clazz) {
        List<FieldMeta> metas = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            FieldEncrypt annotation = field.getAnnotation(FieldEncrypt.class);
            if (annotation == null || !String.class.equals(field.getType())) {
                continue;
            }
            Field hashField = resolveHashField(clazz, field, annotation);
            metas.add(new FieldMeta(field, annotation, hashField));
        }
        return metas;
    }

    private Field resolveHashField(Class<?> clazz, Field field, FieldEncrypt annotation) {
        if (!annotation.searchable()) {
            return null;
        }
        String hashName = annotation.hashField().isEmpty() ? field.getName() + "Hash" : annotation.hashField();
        try {
            return clazz.getDeclaredField(hashName);
        } catch (NoSuchFieldException e) {
            log.warn("@FieldEncrypt(searchable=true) 但实体 {} 缺少盲索引字段 {}（须手写 @Column，不会自动建列），等值查询将不可用", clazz.getSimpleName(), hashName);
            return null;
        }
    }

    @FunctionalInterface
    private interface FieldValueConsumer {
        void accept(FieldMeta meta, String value) throws IllegalAccessException;
    }
}
