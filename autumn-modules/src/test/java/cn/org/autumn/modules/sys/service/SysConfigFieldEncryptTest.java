package cn.org.autumn.modules.sys.service;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.base.EncryptModuleService;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.modules.sys.dao.SysConfigDao;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.redis.SysConfigRedis;
import cn.org.autumn.site.EncryptConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SysConfig paramValue：双读（明文/ENC$）、写密文、API/缓存侧明文。
 */
public class SysConfigFieldEncryptTest {

    private static final byte[] KEY = new byte[32];

    static {
        Arrays.fill(KEY, (byte) 9);
    }

    private FieldEncryptService fieldEncryptService;
    private SysConfigService service;
    private AtomicReference<String> storedValue;
    private AtomicReference<SysConfigEntity> dbRow;

    @BeforeEach
    public void setUp() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        fieldEncryptService = newFieldEncryptService(properties);
        fieldEncryptService.registerEntity(SysConfigEntity.class);

        storedValue = new AtomicReference<>();
        dbRow = new AtomicReference<>();

        SysConfigDao dao = mock(SysConfigDao.class);
        when(dao.queryByKey(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            SysConfigEntity row = dbRow.get();
            if (row == null || !key.equals(row.getParamKey())) {
                return null;
            }
            return copy(row);
        });
        when(dao.updateValueByKey(anyString(), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String value = inv.getArgument(1);
            storedValue.set(value);
            dbRow.set(row(key, value));
            return 1;
        });

        SysConfigRedis redis = mock(SysConfigRedis.class);
        when(redis.get(anyString())).thenReturn(null);
        doAnswer(inv -> null).when(redis).saveOrUpdate(any());
        doAnswer(inv -> null).when(redis).delete(anyString());

        service = new SysConfigService();
        ReflectionTestUtils.setField(service, "encrypt", fieldEncryptService);
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "sysConfigRedis", redis);
    }

    @Test
    public void paramValueAnnotatedAndServiceExtendsEncryptModule() throws Exception {
        Field field = SysConfigEntity.class.getDeclaredField("paramValue");
        Assertions.assertNotNull(field.getAnnotation(FieldEncrypt.class));
        Assertions.assertTrue(EncryptModuleService.class.isAssignableFrom(SysConfigService.class));
        Assertions.assertNull(service.getMenuItems());
        Assertions.assertNull(ReflectionTestUtils.invokeMethod(service, "getMenuItemsInternal"));
    }

    @Test
    public void dualReadAcceptsPlainAndCipher() {
        SysConfigEntity plain = row("K1", "hello-plain");
        fieldEncryptService.onRead(plain);
        Assertions.assertEquals("hello-plain", plain.getParamValue());

        SysConfigEntity cipher = row("K2", "hello-cipher");
        fieldEncryptService.onWrite(cipher);
        Assertions.assertTrue(cipher.getParamValue().startsWith("ENC$v1$"));
        String enc = cipher.getParamValue();
        fieldEncryptService.onRead(cipher);
        Assertions.assertEquals("hello-cipher", cipher.getParamValue());

        SysConfigEntity again = row("K2", enc);
        fieldEncryptService.onRead(again);
        Assertions.assertEquals("hello-cipher", again.getParamValue());
    }

    @Test
    public void updateValueByKeyStoresCipher() {
        service.updateValueByKey("SITE_DOMAIN", "example.com");
        String stored = storedValue.get();
        Assertions.assertNotNull(stored);
        Assertions.assertTrue(stored.startsWith("ENC$v1$"));
        Assertions.assertEquals("example.com", fieldEncryptService.decryptValue(stored));
    }

    @Test
    public void writeDisabled_updateValueByKeyStoresPlain_getValueStillDecryptsLegacyCipher() {
        FieldEncryptProperties off = new FieldEncryptProperties();
        off.setEnabled(false);
        off.setKey(Base64.getEncoder().encodeToString(KEY));
        FieldEncryptService readOnlyDecrypt = newFieldEncryptService(off);
        readOnlyDecrypt.registerEntity(SysConfigEntity.class);
        ReflectionTestUtils.setField(service, "encrypt", readOnlyDecrypt);

        Assertions.assertFalse(readOnlyDecrypt.isWriteEncryptEnabled());
        Assertions.assertTrue(readOnlyDecrypt.isReadDecryptEnabled());

        service.updateValueByKey("PLAIN_KEY", "plain-value");
        Assertions.assertEquals("plain-value", storedValue.get());

        SysConfigEntity cipher = row("LEGACY_KEY", "legacy-secret");
        // force encrypt as if previously migrated
        FieldEncryptProperties on = new FieldEncryptProperties();
        on.setEnabled(true);
        on.setKey(Base64.getEncoder().encodeToString(KEY));
        FieldEncryptService writer = newFieldEncryptService(on);
        writer.registerEntity(SysConfigEntity.class);
        writer.onWrite(cipher);
        Assertions.assertTrue(cipher.getParamValue().startsWith("ENC$v1$"));
        // 业务读路径仍用「写关但可读解密」的 encrypt 实例
        dbRow.set(copy(cipher));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());

        Assertions.assertEquals("legacy-secret", service.getValue("LEGACY_KEY"));
    }

    @Test
    public void getValueReturnsPlainFromDbCipher() {
        SysConfigEntity cipher = row("DEBUG_MODE", "true");
        fieldEncryptService.onWrite(cipher);
        Assertions.assertTrue(cipher.getParamValue().startsWith("ENC$v1$"));
        dbRow.set(cipher);

        String value = service.getValue("DEBUG_MODE");
        Assertions.assertEquals("true", value);

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, SysConfigEntity> map =
                (ConcurrentHashMap<String, SysConfigEntity>) ReflectionTestUtils.getField(service, "map");
        Assertions.assertEquals("true", map.get("DEBUG_MODE").getParamValue());
    }

    @Test
    public void getValueMapHitDecryptsStaleCipher() {
        SysConfigEntity cipher = row("MAP_KEY", "from-map");
        fieldEncryptService.onWrite(cipher);
        Assertions.assertTrue(cipher.getParamValue().startsWith("ENC$v1$"));
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, SysConfigEntity> map =
                (ConcurrentHashMap<String, SysConfigEntity>) ReflectionTestUtils.getField(service, "map");
        map.put("MAP_KEY", copy(cipher));

        Assertions.assertEquals("from-map", service.getValue("MAP_KEY"));
        Assertions.assertEquals("from-map", map.get("MAP_KEY").getParamValue());
    }

    @Test
    public void getObjectDecryptsCipherAndSurvivesGarbage() {
        SysConfigEntity cipher = row("ROBOT_QUOTA_CONFIG", "{\"maxRobotsPerUser\":9}");
        fieldEncryptService.onWrite(cipher);
        dbRow.set(copy(cipher));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());

        cn.org.autumn.model.RobotQuotaConfig cfg =
                service.getObject("ROBOT_QUOTA_CONFIG", cn.org.autumn.model.RobotQuotaConfig.class);
        Assertions.assertNotNull(cfg);
        Assertions.assertEquals(9, cfg.getMaxRobotsPerUser());

        dbRow.set(row("BAD_JSON_KEY", "ENC$v1$not-valid-cipher-payload"));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());
        // 无法解密/非 JSON 时回落默认对象，不抛异常拖垮启动
        cn.org.autumn.model.RobotQuotaConfig fallback =
                service.getObject("BAD_JSON_KEY", cn.org.autumn.model.RobotQuotaConfig.class);
        Assertions.assertNotNull(fallback);
        Assertions.assertEquals(5, fallback.getMaxRobotsPerUser());
    }

    @Test
    public void getByKeyDecryptsForApiPlain() {
        SysConfigEntity cipher = row("LOGGER_LEVEL", "INFO");
        fieldEncryptService.onWrite(cipher);
        dbRow.set(cipher);
        SysConfigEntity got = service.getByKey("LOGGER_LEVEL");
        Assertions.assertEquals("INFO", got.getParamValue());
    }

    @Test
    public void getValue_unresolvedCipher_returnsNullAndDoesNotCache() {
        FieldEncryptProperties noKey = new FieldEncryptProperties();
        noKey.setEnabled(false);
        FieldEncryptService noDecrypt = newFieldEncryptService(noKey);
        noDecrypt.registerEntity(SysConfigEntity.class);
        ReflectionTestUtils.setField(service, "encrypt", noDecrypt);

        dbRow.set(row("CLUSTER_NAMESPACE", "ENC$v1$cipher-blob-not-decryptable"));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());

        Assertions.assertNull(service.getValue("CLUSTER_NAMESPACE"));
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, SysConfigEntity> map =
                (ConcurrentHashMap<String, SysConfigEntity>) ReflectionTestUtils.getField(service, "map");
        Assertions.assertFalse(map.containsKey("CLUSTER_NAMESPACE"));
    }

    @Test
    public void getValue_mapPlainHit_skipsDecrypt_evenWithoutEncrypt() {
        ReflectionTestUtils.setField(service, "encrypt", null);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, SysConfigEntity> map =
                (ConcurrentHashMap<String, SysConfigEntity>) ReflectionTestUtils.getField(service, "map");
        map.put("PLAIN_HIT", row("PLAIN_HIT", "cached-plain"));
        Assertions.assertEquals("cached-plain", service.getValue("PLAIN_HIT"));
        Assertions.assertEquals("cached-plain", service.str("PLAIN_HIT"));
    }

    @Test
    public void jsonOrNull_blankReturnsNull_valueParsesOnceCached() {
        Assertions.assertNull(service.jsonOrNull("MISSING_JSON", cn.org.autumn.model.RobotQuotaConfig.class));

        dbRow.set(row("QUOTA_JSON", "{\"maxRobotsPerUser\":7}"));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());

        cn.org.autumn.model.RobotQuotaConfig first =
                service.jsonOrNull("QUOTA_JSON", cn.org.autumn.model.RobotQuotaConfig.class);
        Assertions.assertNotNull(first);
        Assertions.assertEquals(7, first.getMaxRobotsPerUser());

        // map 已缓存明文；改 db 不应影响已解析的 JSON 缓存引用
        dbRow.set(row("QUOTA_JSON", "{\"maxRobotsPerUser\":99}"));
        cn.org.autumn.model.RobotQuotaConfig cached =
                service.json("QUOTA_JSON", cn.org.autumn.model.RobotQuotaConfig.class);
        Assertions.assertSame(first, cached);
        Assertions.assertEquals(7, cached.getMaxRobotsPerUser());
    }

    @Test
    public void invalidateLocalCache_clearsJsonObjectCache() {
        dbRow.set(row("QUOTA_INV", "{\"maxRobotsPerUser\":3}"));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());

        cn.org.autumn.model.RobotQuotaConfig before =
                service.json("QUOTA_INV", cn.org.autumn.model.RobotQuotaConfig.class);
        Assertions.assertEquals(3, before.getMaxRobotsPerUser());

        dbRow.set(row("QUOTA_INV", "{\"maxRobotsPerUser\":11}"));
        service.invalidateLocalCache("QUOTA_INV");

        cn.org.autumn.model.RobotQuotaConfig after =
                service.json("QUOTA_INV", cn.org.autumn.model.RobotQuotaConfig.class);
        Assertions.assertEquals(11, after.getMaxRobotsPerUser());
        Assertions.assertNotSame(before, after);
    }

    @Test
    public void shortAliases_strBoolNumHas() {
        dbRow.set(row("FLAG_KEY", "true"));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());
        Assertions.assertTrue(service.bool("FLAG_KEY"));
        Assertions.assertTrue(service.bool("MISSING_FLAG", true));
        Assertions.assertFalse(service.bool("MISSING_FLAG", false));

        dbRow.set(row("NUM_KEY", "42"));
        ReflectionTestUtils.setField(service, "map", new ConcurrentHashMap<String, SysConfigEntity>());
        Assertions.assertEquals(42, service.num("NUM_KEY"));
        Assertions.assertEquals(8, service.num("MISSING_NUM", 8));

        Assertions.assertEquals("fallback", service.str("MISSING_STR", "fallback"));
        Assertions.assertFalse(service.has("NO_SUCH_KEY_XYZ"));
    }

    private static SysConfigEntity row(String key, String value) {
        SysConfigEntity e = new SysConfigEntity();
        e.setId(1L);
        e.setParamKey(key);
        e.setParamValue(value);
        e.setStatus(1);
        return e;
    }

    private static SysConfigEntity copy(SysConfigEntity row) {
        SysConfigEntity copy = new SysConfigEntity();
        copy.setId(row.getId());
        copy.setParamKey(row.getParamKey());
        copy.setParamValue(row.getParamValue());
        copy.setStatus(row.getStatus());
        return copy;
    }

    private static FieldEncryptService newFieldEncryptService(FieldEncryptProperties properties) {
        EncryptConfigFactory factory = new EncryptConfigFactory();
        ReflectionTestUtils.setField(factory, "handlers", Collections.emptyList());
        FieldEncryptConfigSource configSource = new FieldEncryptConfigSource();
        ReflectionTestUtils.setField(configSource, "properties", properties);
        ReflectionTestUtils.setField(configSource, "environment", new MockEnvironment());
        ReflectionTestUtils.setField(configSource, "encryptConfigFactory", factory);
        FieldEncryptService svc = new FieldEncryptService();
        ReflectionTestUtils.setField(svc, "configSource", configSource);
        svc.init();
        return svc;
    }
}
