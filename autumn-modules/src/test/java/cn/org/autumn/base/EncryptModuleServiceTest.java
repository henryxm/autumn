package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.crypto.FieldEncryptConfigSource;
import cn.org.autumn.crypto.FieldEncryptKtQueryChainWrapper;
import cn.org.autumn.crypto.FieldEncryptLambdaQueryChainWrapper;
import cn.org.autumn.crypto.FieldEncryptQueryChainWrapper;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.service.CompatibleService;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.table.annotation.Column;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EncryptModuleServiceTest {

    private static final byte[] KEY = new byte[32];

    static {
        Arrays.fill(KEY, (byte) 5);
    }

    private FieldEncryptService fieldEncryptService;
    private TestEncryptModuleService service;

    @Before
    public void setUp() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        fieldEncryptService = newFieldEncryptService(properties);
        fieldEncryptService.registerEntity(EncryptSampleEntity.class);
        service = new TestEncryptModuleService();
        ReflectionTestUtils.setField(service, "encrypt", fieldEncryptService);
    }

    @Test
    public void onReadDecryptsCipher() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "plain-secret";
        fieldEncryptService.onWrite(entity);
        Assert.assertTrue(entity.secret.startsWith("ENC$v1$"));
        service.afterRead(entity);
        Assert.assertEquals("plain-secret", entity.secret);
    }

    @Test
    public void insertRestoresPlainAfterPersist() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "token-hash";
        StubEncryptModuleService stub = new StubEncryptModuleService();
        ReflectionTestUtils.setField(stub, "encrypt", fieldEncryptService);
        Assert.assertTrue(stub.insert(entity) > 0);
        Assert.assertEquals("token-hash", entity.secret);
        Assert.assertTrue(stub.persist.secret.startsWith("ENC$v1$"));
    }

    @Test
    public void restoreAfterWriteIsIdempotent() {
        EncryptSampleEntity entity = new EncryptSampleEntity();
        entity.secret = "plain-secret";
        fieldEncryptService.onWrite(entity);
        fieldEncryptService.restoreAfterWrite(entity);
        Assert.assertEquals("plain-secret", entity.secret);
        fieldEncryptService.restoreAfterWrite(entity);
        Assert.assertEquals("plain-secret", entity.secret);
    }

    @Test
    public void overridesAllCompatibleSelectMethods() {
        Set<String> expected = new HashSet<>();
        for (Method method : CompatibleService.class.getMethods()) {
            if (method.getName().startsWith("select") && !"selectCount".equals(method.getName())) {
                expected.add(method.getName());
            }
        }
        Set<String> actual = declaredEncryptReadMethodNames("select");
        Assert.assertTrue("EncryptModuleService 须覆盖 CompatibleService 全部 select*（除 selectCount）", actual.containsAll(expected));
    }

    @Test
    public void overridesAllRepositoryReadMethods() {
        Set<String> expected = repositoryReadMethodNames();
        Set<String> actual = declaredEncryptReadMethodNames(null);
        Assert.assertTrue("EncryptModuleService 须覆盖 IRepository 全部实体读方法", actual.containsAll(expected));
    }

    @Test
    public void lambdaQueryReturnsEncryptChainWrapper() {
        Assert.assertTrue(service.lambdaQuery() instanceof FieldEncryptLambdaQueryChainWrapper);
        Assert.assertTrue(service.query() instanceof FieldEncryptQueryChainWrapper);
        Assert.assertTrue(service.ktQuery() instanceof FieldEncryptKtQueryChainWrapper);
    }

    @Test
    public void overridesChainQueryEntryPoints() throws Exception {
        Assert.assertNotNull(EncryptModuleService.class.getMethod("lambdaQuery"));
        Assert.assertNotNull(EncryptModuleService.class.getMethod("lambdaQuery", Object.class));
        Assert.assertNotNull(EncryptModuleService.class.getMethod("query"));
        Assert.assertNotNull(EncryptModuleService.class.getMethod("ktQuery"));
    }

    @Test
    public void chainQueryTerminalMethodsDecrypt() {
        EncryptSampleEntity cipher = new EncryptSampleEntity();
        cipher.secret = "chain-plain";
        fieldEncryptService.onWrite(cipher);
        Assert.assertTrue(cipher.secret.startsWith("ENC$v1$"));

        ChainQueryTestService chainService = new ChainQueryTestService(singleRowMapper(cipher));
        ReflectionTestUtils.setField(chainService, "encrypt", fieldEncryptService);

        Assert.assertEquals("chain-plain", chainService.lambdaQuery().one().secret);
        Assert.assertEquals("chain-plain", chainService.lambdaQuery().list().get(0).secret);
        Assert.assertEquals("chain-plain", chainService.lambdaQuery().oneOpt().get().secret);
        Assert.assertEquals("chain-plain", chainService.query().one().secret);
        Assert.assertEquals("chain-plain", chainService.query().lambda().one().secret);
        Assert.assertEquals("chain-plain", chainService.lambdaQuery(new EncryptSampleEntity()).list().get(0).secret);
        Assert.assertEquals("chain-plain", chainService.ktQuery().one().secret);

        Page<EncryptSampleEntity> page = new Page<>(1, 10);
        chainService.lambdaQuery().page(page);
        Assert.assertEquals("chain-plain", page.getRecords().get(0).secret);
    }

    @Test
    public void chainQuerySkipsDecryptWhenEntityNotEncrypted() {
        PlainEntity row = new PlainEntity();
        row.note = "ENC$v1$looks-like-cipher";
        PlainChainQueryTestService chainService = new PlainChainQueryTestService(plainRowMapper(row));
        ReflectionTestUtils.setField(chainService, "encrypt", fieldEncryptService);
        Assert.assertEquals("ENC$v1$looks-like-cipher", chainService.lambdaQuery().one().note);
    }

    private static BaseMapper<EncryptSampleEntity> singleRowMapper(EncryptSampleEntity row) {
        InvocationHandler handler = (proxy, method, args) -> dispatchChainQueryMapper(method, row, args);
        return (BaseMapper<EncryptSampleEntity>) Proxy.newProxyInstance(BaseMapper.class.getClassLoader(), new Class[]{BaseMapper.class}, handler);
    }

    private static BaseMapper<PlainEntity> plainRowMapper(PlainEntity row) {
        InvocationHandler handler = (proxy, method, args) -> dispatchChainQueryMapper(method, row, args);
        return (BaseMapper<PlainEntity>) Proxy.newProxyInstance(BaseMapper.class.getClassLoader(), new Class[]{BaseMapper.class}, handler);
    }

    private static Object dispatchChainQueryMapper(Method method, Object row, Object[] args) {
        String name = method.getName();
        if ("selectList".equals(name)) {
            return Collections.singletonList(row);
        }
        if ("selectOne".equals(name)) {
            return row;
        }
        if ("selectPage".equals(name) && args != null && args.length >= 1 && args[0] instanceof IPage) {
            @SuppressWarnings("unchecked")
            IPage<Object> page = (IPage<Object>) args[0];
            page.setRecords(Collections.singletonList(row));
            return page;
        }
        if ("selectCount".equals(name)) {
            return 1L;
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (List.class.isAssignableFrom(returnType)) {
            return Collections.emptyList();
        }
        return null;
    }

    private static Set<String> declaredEncryptReadMethodNames(String prefix) {
        Set<String> actual = new HashSet<>();
        for (Method method : EncryptModuleService.class.getDeclaredMethods()) {
            if (prefix != null) {
                if (method.getName().startsWith(prefix) && !"selectCount".equals(method.getName())) {
                    actual.add(method.getName());
                }
            } else if (isRepositoryReadMethod(method.getName())) {
                actual.add(method.getName());
            }
        }
        return actual;
    }

    private static Set<String> repositoryReadMethodNames() {
        Set<String> expected = new HashSet<>();
        for (Method method : IRepository.class.getMethods()) {
            if (isRepositoryReadMethod(method.getName())) {
                expected.add(method.getName());
            }
        }
        return expected;
    }

    private static boolean isRepositoryReadMethod(String name) {
        if ("getBaseMapper".equals(name) || "getEntityClass".equals(name)) {
            return false;
        }
        if ("getById".equals(name) || "getOptById".equals(name) || "getMap".equals(name) || "getObj".equals(name)) {
            return true;
        }
        if (name.startsWith("getOne")) {
            return true;
        }
        if (name.startsWith("list") && !name.contains("Chain")) {
            return true;
        }
        return name.startsWith("page");
    }

    private static FieldEncryptService newFieldEncryptService(FieldEncryptProperties properties) {
        EncryptConfigFactory factory = new EncryptConfigFactory();
        ReflectionTestUtils.setField(factory, "handlers", Collections.emptyList());
        FieldEncryptConfigSource configSource = new FieldEncryptConfigSource();
        ReflectionTestUtils.setField(configSource, "properties", properties);
        ReflectionTestUtils.setField(configSource, "encryptConfigFactory", factory);
        FieldEncryptService service = new FieldEncryptService();
        ReflectionTestUtils.setField(service, "configSource", configSource);
        service.init();
        return service;
    }

    static class EncryptSampleEntity {
        @Column
        @FieldEncrypt
        String secret;
    }

    static class PlainEntity {
        @Column
        String note;
    }

    static class TestEncryptModuleService extends EncryptModuleService<BaseMapper<Object>, Object> {
    }

    static class ChainQueryTestService extends EncryptModuleService<BaseMapper<EncryptSampleEntity>, EncryptSampleEntity> {
        private final BaseMapper<EncryptSampleEntity> mapper;

        ChainQueryTestService(BaseMapper<EncryptSampleEntity> mapper) {
            this.mapper = mapper;
        }

        @Override
        public BaseMapper<EncryptSampleEntity> getBaseMapper() {
            return mapper;
        }
    }

    static class PlainChainQueryTestService extends EncryptModuleService<BaseMapper<PlainEntity>, PlainEntity> {
        private final BaseMapper<PlainEntity> mapper;

        PlainChainQueryTestService(BaseMapper<PlainEntity> mapper) {
            this.mapper = mapper;
        }

        @Override
        public BaseMapper<PlainEntity> getBaseMapper() {
            return mapper;
        }
    }

    static class StubEncryptModuleService extends EncryptModuleService<BaseMapper<EncryptSampleEntity>, EncryptSampleEntity> {
        EncryptSampleEntity persist;

        @Override
        public int insert(EncryptSampleEntity entity) {
            encrypt.onWrite(entity);
            try {
                persist = entity;
                return 1;
            } finally {
                restoreAfterWrite(entity);
            }
        }
    }
}
