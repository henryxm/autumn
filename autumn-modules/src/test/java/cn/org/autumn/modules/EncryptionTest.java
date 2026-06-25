package cn.org.autumn.modules;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.*;
import cn.org.autumn.service.AesService;
import cn.org.autumn.service.CacheService;
import cn.org.autumn.service.RsaService;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.utils.AES;
import cn.org.autumn.utils.RsaUtil;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RSA和AES组合加密方案测试服务
 * <p>
 * 提供完整的测试用例，涵盖所有接口和场景
 * 包括正常流程、异常场景、边界情况等
 *
 * @author Autumn
 */
@Slf4j
public class EncryptionTest {

    private RsaService rsaService;

    private AesService aesService;

    private final Map<String, Object> memoryCache = new ConcurrentHashMap<>();

    @Before
    public void setUp() {
        memoryCache.clear();
        CacheService cacheService = mock(CacheService.class);
        when(cacheService.compute(any(), any(Supplier.class), any(cn.org.autumn.config.CacheConfig.class))).thenAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            Supplier<?> supplier = invocation.getArgument(1);
            cn.org.autumn.config.CacheConfig config = invocation.getArgument(2);
            String cacheKey = config.getName() + ":" + key;
            Object existing = memoryCache.get(cacheKey);
            if (existing != null)
                return existing;
            Object value = supplier.get();
            memoryCache.put(cacheKey, value);
            return value;
        });
        when(cacheService.get(any(cn.org.autumn.config.CacheConfig.class), any())).thenAnswer(invocation -> {
            cn.org.autumn.config.CacheConfig config = invocation.getArgument(0);
            Object key = invocation.getArgument(1);
            return memoryCache.get(config.getName() + ":" + key);
        });
        doAnswer(invocation -> {
            cn.org.autumn.config.CacheConfig config = invocation.getArgument(0);
            Object key = invocation.getArgument(1);
            Object value = invocation.getArgument(2);
            memoryCache.put(config.getName() + ":" + key, value);
            return null;
        }).when(cacheService).put(any(cn.org.autumn.config.CacheConfig.class), any(), any());
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            Object key = invocation.getArgument(1);
            memoryCache.remove(name + ":" + key);
            return null;
        }).when(cacheService).remove(anyString(), any());

        EncryptConfigFactory encryptConfigFactory = new EncryptConfigFactory();
        ReflectionTestUtils.setField(encryptConfigFactory, "handlers", Collections.emptyList());

        rsaService = new RsaService();
        ReflectionTestUtils.setField(rsaService, "cacheService", cacheService);
        ReflectionTestUtils.setField(rsaService, "encryptConfigFactory", encryptConfigFactory);

        aesService = new AesService();
        ReflectionTestUtils.setField(aesService, "cacheService", cacheService);
        ReflectionTestUtils.setField(aesService, "encryptConfigFactory", encryptConfigFactory);
        ReflectionTestUtils.setField(aesService, "gson", new Gson());
    }

    /**
     * 测试结果
     */
    public static class TestResult {
        private String testName;
        private boolean success;
        private String message;
        private Exception exception;
        private Map<String, Object> data = new HashMap<>();

        public TestResult(String testName) {
            this.testName = testName;
        }

        public static TestResult success(String testName, String message) {
            TestResult result = new TestResult(testName);
            result.success = true;
            result.message = message;
            return result;
        }

        public static TestResult failure(String testName, String message, Exception e) {
            TestResult result = new TestResult(testName);
            result.success = false;
            result.message = message;
            result.exception = e;
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", testName, message);
        }

        public TestResult addData(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        // Getters
        public String getTestName() {
            return testName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Exception getException() {
            return exception;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    @Test
    public void test() {
        List<TestResult> results = runAllTests();
        List<TestResult> failures = new ArrayList<>();
        for (TestResult result : results) {
            if (!result.isSuccess())
                failures.add(result);
        }
        if (!failures.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("加密测试失败 ").append(failures.size()).append(" 项:");
            for (TestResult failure : failures) {
                message.append("\n- ").append(failure.getTestName()).append(": ").append(failure.getMessage());
                if (failure.getException() != null)
                    message.append(" (").append(failure.getException().getClass().getSimpleName()).append(")");
            }
            fail(message.toString());
        }
    }

    /**
     * 运行所有测试用例
     *
     * @return 测试结果列表
     */
    public List<TestResult> runAllTests() {
        log.debug(repeat("=", 80));
        log.debug("开始运行RSA和AES组合加密方案测试");
        log.debug(repeat("=", 80));

        List<TestResult> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // 1. RSA密钥对管理测试
        log.debug("\n【测试组 1/8】RSA密钥对管理测试");
        log.debug(repeat("-", 80));
        List<TestResult> rsaKeyPairResults = testRsaKeyPairManagement();
        results.addAll(rsaKeyPairResults);
        logTestGroupResults("RSA密钥对管理", rsaKeyPairResults);

        // 2. 客户端公钥管理测试
        log.debug("\n【测试组 2/8】客户端公钥管理测试");
        log.debug(repeat("-", 80));
        List<TestResult> clientKeyResults = testClientPublicKeyManagement();
        results.addAll(clientKeyResults);
        logTestGroupResults("客户端公钥管理", clientKeyResults);

        // 3. AES密钥管理测试
        log.debug("\n【测试组 3/8】AES密钥管理测试");
        log.debug(repeat("-", 80));
        List<TestResult> aesKeyResults = testAesKeyManagement();
        results.addAll(aesKeyResults);
        logTestGroupResults("AES密钥管理", aesKeyResults);

        // 4. RSA加密解密测试
        log.debug("\n【测试组 4/8】RSA加密解密测试");
        log.debug(repeat("-", 80));
        List<TestResult> rsaEncryptResults = testRsaEncryptionDecryption();
        results.addAll(rsaEncryptResults);
        logTestGroupResults("RSA加密解密", rsaEncryptResults);

        // 5. AES加密解密测试
        log.debug("\n【测试组 5/8】AES加密解密测试");
        log.debug(repeat("-", 80));
        List<TestResult> aesEncryptResults = testAesEncryptionDecryption();
        results.addAll(aesEncryptResults);
        logTestGroupResults("AES加密解密", aesEncryptResults);

        // 6. RSA+AES组合流程测试
        log.debug("\n【测试组 6/8】RSA+AES组合流程测试");
        log.debug(repeat("-", 80));
        List<TestResult> combinedResults = testRsaAesCombinedFlow();
        results.addAll(combinedResults);
        logTestGroupResults("RSA+AES组合流程", combinedResults);

        // 7. 错误场景测试
        log.debug("\n【测试组 7/8】错误场景测试");
        log.debug(repeat("-", 80));
        List<TestResult> errorResults = testErrorScenarios();
        results.addAll(errorResults);
        logTestGroupResults("错误场景", errorResults);

        // 8. 边界情况测试
        log.debug("\n【测试组 8/8】边界情况测试");
        log.debug(repeat("-", 80));
        List<TestResult> boundaryResults = testBoundaryCases();
        results.addAll(boundaryResults);
        logTestGroupResults("边界情况", boundaryResults);

        // 输出测试总结
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logTestSummary(results, duration);

        return results;
    }

    /**
     * 输出测试组结果
     */
    private void logTestGroupResults(String groupName, List<TestResult> results) {
        long successCount = results.stream().filter(TestResult::isSuccess).count();
        long failureCount = results.size() - successCount;
        String status = failureCount == 0 ? "✓ 全部通过" : "✗ 有失败";
        double successRate = results.isEmpty() ? 0 : (successCount * 100.0 / results.size());

        log.debug("测试组 [{}] 完成: {} (成功: {}, 失败: {}, 总计: {}, 成功率: {:.2f}%)", groupName, status, successCount, failureCount, results.size(), successRate);

        // 输出每个测试用例的结果
        for (TestResult result : results) {
            if (result.isSuccess()) {
                log.info("  ✓ {}: {}", result.getTestName(), result.getMessage());
            } else {
                log.warn("  ✗ {}: {}", result.getTestName(), result.getMessage());
                if (result.getException() != null) {
                    log.warn("    异常: {} - {}",
                            result.getException().getClass().getSimpleName(),
                            result.getException().getMessage());
                }
            }
        }
    }

    /**
     * 输出测试总结
     */
    private void logTestSummary(List<TestResult> results, long duration) {
        long successCount = results.stream().filter(TestResult::isSuccess).count();
        long failureCount = results.size() - successCount;
        double successRate = results.isEmpty() ? 0 : (successCount * 100.0 / results.size());

        log.debug("\n" + repeat("=", 80));
        log.debug("测试总结");
        log.debug(repeat("=", 80));
        log.debug("总测试数: {}", results.size());
        log.debug("成功: {} ({:.2f}%)", successCount, successRate);
        log.debug("失败: {} ({:.2f}%)", failureCount, 100 - successRate);
        log.debug("执行时间: {} ms ({:.2f} 秒)", duration, duration / 1000.0);
        log.debug(repeat("=", 80));

        if (failureCount == 0) {
            log.debug("🎉 所有测试用例全部通过！");
        } else {
            log.warn("⚠️  有 {} 个测试用例失败，请检查上述详细信息", failureCount);
        }
    }

    /**
     * 测试1: RSA密钥对管理
     */
    private List<TestResult> testRsaKeyPairManagement() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.debug("开始RSA密钥对管理测试，UUID: {}", uuid);

        try {
            // 1.1 获取服务端公钥（首次获取，应该生成新的密钥对）
            log.debug("  测试1.1: 获取服务端公钥（首次）");
            RsaKey keyPair1 = rsaService.getKey(uuid);
            if (keyPair1 == null || StringUtils.isBlank(keyPair1.getPublicKey())) {
                log.error("  测试1.1失败: 密钥对为空");
                results.add(TestResult.failure("获取服务端公钥-首次", "密钥对为空", null));
            } else {
                log.debug("  测试1.1通过: 成功生成密钥对，过期时间: {}", keyPair1.getExpireTime());
                results.add(TestResult.success("获取服务端公钥-首次", "成功生成密钥对")
                        .addData("publicKey", keyPair1.getPublicKey().substring(0, 50) + "...")
                        .addData("expireTime", keyPair1.getExpireTime()));
            }

            // 1.2 再次获取相同UUID的公钥（应该返回缓存的密钥对）
            log.debug("  测试1.2: 获取服务端公钥（缓存）");
            RsaKey keyPair2 = rsaService.getKey(uuid);
            if (keyPair2 == null || !keyPair2.getPublicKey().equals(keyPair1.getPublicKey())) {
                log.error("  测试1.2失败: 密钥对不一致或为空");
                results.add(TestResult.failure("获取服务端公钥-缓存", "密钥对不一致或为空", null));
            } else {
                log.debug("  测试1.2通过: 成功返回缓存的密钥对");
                results.add(TestResult.success("获取服务端公钥-缓存", "成功返回缓存的密钥对"));
            }

            // 1.3 测试不同UUID生成不同的密钥对
            log.debug("  测试1.3: 不同UUID生成不同密钥对");
            String uuid2 = UUID.randomUUID().toString();
            RsaKey keyPair3 = rsaService.getKey(uuid2);
            if (keyPair3 == null || keyPair3.getPublicKey().equals(keyPair1.getPublicKey())) {
                log.error("  测试1.3失败: 不同UUID生成了相同的密钥对");
                results.add(TestResult.failure("获取服务端公钥-不同UUID", "不同UUID生成了相同的密钥对", null));
            } else {
                log.debug("  测试1.3通过: 不同UUID生成了不同的密钥对");
                results.add(TestResult.success("获取服务端公钥-不同UUID", "不同UUID生成了不同的密钥对"));
            }

            // 1.4 测试密钥对过期时间
            log.debug("  测试1.4: 密钥对过期时间验证");
            if (keyPair1.getExpireTime() != null && keyPair1.getExpireTime() > System.currentTimeMillis()) {
                log.debug("  测试1.4通过: 过期时间设置正确");
                results.add(TestResult.success("密钥对过期时间", "过期时间设置正确"));
            } else {
                log.error("  测试1.4失败: 过期时间设置错误");
                results.add(TestResult.failure("密钥对过期时间", "过期时间设置错误", null));
            }

        } catch (Exception e) {
            log.error("RSA密钥对管理测试异常", e);
            results.add(TestResult.failure("RSA密钥对管理", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试2: 客户端公钥管理
     */
    private List<TestResult> testClientPublicKeyManagement() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.debug("开始客户端公钥管理测试，UUID: {}", uuid);

        try {
            // 2.1 生成客户端RSA密钥对
            log.debug("  测试2.1: 生成客户端RSA密钥对");
            RsaKey clientKeyPair = RsaUtil.generate();
            String clientPublicKey = clientKeyPair.getPublicKey();
            log.debug("  测试2.1通过: 成功生成客户端密钥对");

            // 2.2 上传客户端公钥（不指定过期时间）
            log.debug("  测试2.2: 上传客户端公钥（默认过期时间）");
            RsaKey savedKey1 = rsaService.savePublicKey(uuid, clientPublicKey, null);
            if (savedKey1 == null || !savedKey1.getPublicKey().equals(clientPublicKey)) {
                log.error("  测试2.2失败: 保存失败或公钥不匹配");
                results.add(TestResult.failure("上传客户端公钥-默认过期时间", "保存失败或公钥不匹配", null));
            } else {
                log.debug("  测试2.2通过: 成功保存客户端公钥，过期时间: {}", savedKey1.getExpireTime());
                results.add(TestResult.success("上传客户端公钥-默认过期时间", "成功保存客户端公钥")
                        .addData("expireTime", savedKey1.getExpireTime()));
            }

            // 2.3 检查客户端公钥是否存在
            log.debug("  测试2.3: 检查客户端公钥是否存在");
            boolean hasValidKey = rsaService.hasValidClientPublicKey(uuid);
            if (!hasValidKey) {
                log.error("  测试2.3失败: 公钥应该存在但检查失败");
                results.add(TestResult.failure("检查客户端公钥-存在", "公钥应该存在但检查失败", null));
            } else {
                log.debug("  测试2.3通过: 成功检查到公钥存在");
                results.add(TestResult.success("检查客户端公钥-存在", "成功检查到公钥存在"));
            }

            // 2.4 获取客户端公钥
            log.debug("  测试2.4: 获取客户端公钥");
            RsaKey retrievedKey = rsaService.getClientPublicKey(uuid);
            if (retrievedKey == null || !retrievedKey.getPublicKey().equals(clientPublicKey)) {
                log.error("  测试2.4失败: 获取的公钥不匹配");
                results.add(TestResult.failure("获取客户端公钥", "获取的公钥不匹配", null));
            } else {
                log.debug("  测试2.4通过: 成功获取客户端公钥");
                results.add(TestResult.success("获取客户端公钥", "成功获取客户端公钥"));
            }

            // 2.5 上传客户端公钥（指定过期时间）
            log.debug("  测试2.5: 上传客户端公钥（自定义过期时间）");
            String uuid2 = UUID.randomUUID().toString();
            RsaKey clientKeyPair2 = RsaUtil.generate();
            long customExpireTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000L; // 24小时后
            RsaKey savedKey2 = rsaService.savePublicKey(uuid2, clientKeyPair2.getPublicKey(), customExpireTime);
            if (savedKey2 == null || savedKey2.getExpireTime() != customExpireTime) {
                log.error("  测试2.5失败: 过期时间设置失败");
                results.add(TestResult.failure("上传客户端公钥-自定义过期时间", "过期时间设置失败", null));
            } else {
                log.debug("  测试2.5通过: 成功使用自定义过期时间");
                results.add(TestResult.success("上传客户端公钥-自定义过期时间", "成功使用自定义过期时间"));
            }

            // 2.6 测试更新客户端公钥
            log.debug("  测试2.6: 更新客户端公钥");
            RsaKey newKeyPair = RsaUtil.generate();
            RsaKey updatedKey = rsaService.savePublicKey(uuid, newKeyPair.getPublicKey(), null);
            if (updatedKey == null || !updatedKey.getPublicKey().equals(newKeyPair.getPublicKey())) {
                log.error("  测试2.6失败: 更新失败");
                results.add(TestResult.failure("更新客户端公钥", "更新失败", null));
            } else {
                log.debug("  测试2.6通过: 成功更新客户端公钥");
                results.add(TestResult.success("更新客户端公钥", "成功更新客户端公钥"));
            }

        } catch (Exception e) {
            log.error("客户端公钥管理测试异常", e);
            results.add(TestResult.failure("客户端公钥管理", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试3: AES密钥管理
     */
    private List<TestResult> testAesKeyManagement() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.debug("开始AES密钥管理测试，UUID: {}", uuid);

        try {
            // 3.1 生成AES密钥
            log.debug("  测试3.1: 生成AES密钥");
            AesKey aesKey1 = aesService.getKey(uuid);
            if (aesKey1 == null || StringUtils.isBlank(aesKey1.getKey()) || StringUtils.isBlank(aesKey1.getVector())) {
                log.error("  测试3.1失败: 密钥或向量为空");
                results.add(TestResult.failure("生成AES密钥", "密钥或向量为空", null));
            } else {
                log.debug("  测试3.1通过: 成功生成AES密钥和向量，密钥长度: {}, 向量长度: {}, 过期时间: {}",
                        aesKey1.getKey().length(), aesKey1.getVector().length(), aesKey1.getExpireTime());
                results.add(TestResult.success("生成AES密钥", "成功生成AES密钥和向量")
                        .addData("keyLength", aesKey1.getKey().length())
                        .addData("vectorLength", aesKey1.getVector().length())
                        .addData("expireTime", aesKey1.getExpireTime()));
            }

            // 3.2 获取AES密钥（应该返回缓存的密钥）
            log.debug("  测试3.2: 获取AES密钥（缓存）");
            AesKey aesKey2 = aesService.getKey(uuid);
            if (aesKey2 == null || !aesKey2.getKey().equals(aesKey1.getKey())) {
                log.error("  测试3.2失败: 密钥不一致或为空");
                results.add(TestResult.failure("获取AES密钥-缓存", "密钥不一致或为空", null));
            } else {
                log.debug("  测试3.2通过: 成功返回缓存的AES密钥");
                results.add(TestResult.success("获取AES密钥-缓存", "成功返回缓存的AES密钥"));
            }

            // 3.3 测试不同UUID生成不同的AES密钥
            log.debug("  测试3.3: 不同UUID生成不同AES密钥");
            String uuid2 = UUID.randomUUID().toString();
            AesKey aesKey3 = aesService.getKey(uuid2);
            if (aesKey3 == null || aesKey3.getKey().equals(aesKey1.getKey())) {
                log.error("  测试3.3失败: 不同UUID生成了相同的密钥");
                results.add(TestResult.failure("生成AES密钥-不同UUID", "不同UUID生成了相同的密钥", null));
            } else {
                log.debug("  测试3.3通过: 不同UUID生成了不同的密钥");
                results.add(TestResult.success("生成AES密钥-不同UUID", "不同UUID生成了不同的密钥"));
            }

            // 3.4 检查AES密钥是否存在
            log.debug("  测试3.4: 检查AES密钥是否存在");
            boolean hasValidAesKey = aesService.has(uuid);
            if (!hasValidAesKey) {
                log.error("  测试3.4失败: 密钥应该存在但检查失败");
                results.add(TestResult.failure("检查AES密钥-存在", "密钥应该存在但检查失败", null));
            } else {
                log.debug("  测试3.4通过: 成功检查到AES密钥存在");
                results.add(TestResult.success("检查AES密钥-存在", "成功检查到AES密钥存在"));
            }

            // 3.5 测试密钥过期时间
            log.debug("  测试3.5: AES密钥过期时间验证");
            if (aesKey1.getExpireTime() != null && aesKey1.getExpireTime() > System.currentTimeMillis()) {
                log.debug("  测试3.5通过: 过期时间设置正确");
                results.add(TestResult.success("AES密钥过期时间", "过期时间设置正确"));
            } else {
                log.error("  测试3.5失败: 过期时间设置错误");
                results.add(TestResult.failure("AES密钥过期时间", "过期时间设置错误", null));
            }

            // 3.6 移除AES密钥
            log.debug("  测试3.6: 移除AES密钥");
            aesService.remove(uuid);
            boolean hasKeyAfterRemove = aesService.has(uuid);
            if (hasKeyAfterRemove) {
                log.error("  测试3.6失败: 密钥移除失败");
                results.add(TestResult.failure("移除AES密钥", "密钥移除失败", null));
            } else {
                log.debug("  测试3.6通过: 成功移除AES密钥");
                results.add(TestResult.success("移除AES密钥", "成功移除AES密钥"));
            }

        } catch (Exception e) {
            log.error("AES密钥管理测试异常", e);
            results.add(TestResult.failure("AES密钥管理", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试4: RSA加密解密
     */
    private List<TestResult> testRsaEncryptionDecryption() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.debug("开始RSA加密解密测试，UUID: {}", uuid);

        try {
            // 4.1 获取服务端密钥对
            log.debug("  测试4.1: 获取服务端密钥对");
            RsaKey serverKeyPair = rsaService.getKey(uuid);
            String serverPublicKey = serverKeyPair.getPublicKey();
            String serverPrivateKey = serverKeyPair.getPrivateKey();
            log.debug("  测试4.1通过: 成功获取服务端密钥对");

            // 4.2 生成客户端密钥对
            log.debug("  测试4.2: 生成客户端密钥对");
            RsaKey clientKeyPair = RsaUtil.generate();
            String clientPublicKey = clientKeyPair.getPublicKey();
            String clientPrivateKey = clientKeyPair.getPrivateKey();
            log.debug("  测试4.2通过: 成功生成客户端密钥对");

            // 4.3 保存客户端公钥
            log.debug("  测试4.3: 保存客户端公钥");
            rsaService.savePublicKey(uuid, clientPublicKey, null);
            log.debug("  测试4.3通过: 成功保存客户端公钥");

            // 4.4 测试服务端公钥加密，服务端私钥解密
            log.debug("  测试4.4: 服务端密钥对加解密");
            String testData1 = "测试数据：RSA加密解密";
            String encrypted1 = RsaUtil.encrypt(testData1, serverPublicKey);
            String decrypted1 = RsaUtil.decrypt(encrypted1, serverPrivateKey);
            if (!testData1.equals(decrypted1)) {
                log.error("  测试4.4失败: 解密结果不匹配");
                results.add(TestResult.failure("RSA加密解密-服务端", "解密结果不匹配", null));
            } else {
                log.debug("  测试4.4通过: 服务端密钥对加解密正常");
                results.add(TestResult.success("RSA加密解密-服务端", "服务端密钥对加解密正常"));
            }

            // 4.5 测试客户端公钥加密，客户端私钥解密
            log.debug("  测试4.5: 客户端密钥对加解密");
            String encrypted2 = RsaUtil.encrypt(testData1, clientPublicKey);
            String decrypted2 = RsaUtil.decrypt(encrypted2, clientPrivateKey);
            if (!testData1.equals(decrypted2)) {
                log.error("  测试4.5失败: 解密结果不匹配");
                results.add(TestResult.failure("RSA加密解密-客户端", "解密结果不匹配", null));
            } else {
                log.debug("  测试4.5通过: 客户端密钥对加解密正常");
                results.add(TestResult.success("RSA加密解密-客户端", "客户端密钥对加解密正常"));
            }

            // 4.6 测试使用RsaService加密（使用客户端公钥）
            log.debug("  测试4.6: 使用RsaService加密");
            String encrypted3 = rsaService.encrypt(testData1, uuid);
            String decrypted3 = RsaUtil.decrypt(encrypted3, clientPrivateKey);
            if (!testData1.equals(decrypted3)) {
                log.error("  测试4.6失败: 使用RsaService加密后解密失败");
                results.add(TestResult.failure("RSA加密-RsaService", "使用RsaService加密后解密失败", null));
            } else {
                log.debug("  测试4.6通过: 使用RsaService加密正常");
                results.add(TestResult.success("RSA加密-RsaService", "使用RsaService加密正常"));
            }

            // 4.7 测试使用RsaService解密（使用服务端私钥）
            log.debug("  测试4.7: 使用RsaService解密");
            String encrypted4 = RsaUtil.encrypt(testData1, serverPublicKey);
            DefaultEncrypt encryptObj = new DefaultEncrypt();
            encryptObj.setCiphertext(encrypted4);
            encryptObj.setSession(uuid);
            String decrypted4 = rsaService.decrypt(encryptObj);
            if (!testData1.equals(decrypted4)) {
                log.error("  测试4.7失败: 使用RsaService解密失败");
                results.add(TestResult.failure("RSA解密-RsaService", "使用RsaService解密失败", null));
            } else {
                log.debug("  测试4.7通过: 使用RsaService解密正常");
                results.add(TestResult.success("RSA解密-RsaService", "使用RsaService解密正常"));
            }

            // 4.8 测试长文本加密解密
            log.debug("  测试4.8: RSA长文本加解密");
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longText.append("这是一段很长的测试文本，用于测试RSA加密解密对长文本的支持。");
            }
            String longTestData = longText.toString();
            String encryptedLong = RsaUtil.encrypt(longTestData, serverPublicKey);
            String decryptedLong = RsaUtil.decrypt(encryptedLong, serverPrivateKey);
            if (!longTestData.equals(decryptedLong)) {
                log.error("  测试4.8失败: 长文本解密结果不匹配");
                results.add(TestResult.failure("RSA加密解密-长文本", "长文本解密结果不匹配", null));
            } else {
                log.debug("  测试4.8通过: 长文本加解密正常");
                results.add(TestResult.success("RSA加密解密-长文本", "长文本加解密正常"));
            }

        } catch (Exception e) {
            log.error("RSA加密解密测试异常", e);
            results.add(TestResult.failure("RSA加密解密", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试5: AES加密解密
     */
    private List<TestResult> testAesEncryptionDecryption() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.debug("开始AES加密解密测试，UUID: {}", uuid);

        try {
            // 5.1 生成AES密钥
            log.debug("  测试5.1: 生成AES密钥");
            AesKey aesKey = aesService.getKey(uuid);
            String aesKeyBase64 = aesKey.getKey();
            String aesVectorBase64 = aesKey.getVector();
            log.debug("  测试5.1通过: 成功生成AES密钥");

            // 5.2 测试AES加密解密
            log.debug("  测试5.2: AES基本加解密");
            String testData = "测试数据：AES加密解密";
            String encrypted = AES.encrypt(testData, aesKeyBase64, aesVectorBase64);
            String decrypted = AES.decrypt(encrypted, aesKeyBase64, aesVectorBase64);
            if (!testData.equals(decrypted)) {
                log.error("  测试5.2失败: 解密结果不匹配");
                results.add(TestResult.failure("AES加密解密-基本", "解密结果不匹配", null));
            } else {
                log.debug("  测试5.2通过: AES加解密正常");
                results.add(TestResult.success("AES加密解密-基本", "AES加解密正常"));
            }

            // 5.3 测试使用AesService加密解密
            log.debug("  测试5.3: 使用AesService加解密");
            String encrypted2 = aesService.encrypt(testData, uuid);
            String decrypted2 = aesService.decrypt(encrypted2, uuid);
            if (!testData.equals(decrypted2)) {
                log.error("  测试5.3失败: 使用AesService加解密失败");
                results.add(TestResult.failure("AES加密解密-AesService", "使用AesService加解密失败", null));
            } else {
                log.debug("  测试5.3通过: 使用AesService加解密正常");
                results.add(TestResult.success("AES加密解密-AesService", "使用AesService加解密正常"));
            }

            // 5.4 测试长文本加密解密
            log.debug("  测试5.4: AES长文本加解密");
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longText.append("这是一段很长的测试文本，用于测试AES加密解密对长文本的支持。");
            }
            String longTestData = longText.toString();
            String encryptedLong = aesService.encrypt(longTestData, uuid);
            String decryptedLong = aesService.decrypt(encryptedLong, uuid);
            if (!longTestData.equals(decryptedLong)) {
                log.error("  测试5.4失败: 长文本解密结果不匹配");
                results.add(TestResult.failure("AES加密解密-长文本", "长文本解密结果不匹配", null));
            } else {
                log.debug("  测试5.4通过: 长文本加解密正常");
                results.add(TestResult.success("AES加密解密-长文本", "长文本加解密正常"));
            }

            // 5.5 测试特殊字符
            log.debug("  测试5.5: AES特殊字符加解密");
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~中文测试";
            String encryptedSpecial = aesService.encrypt(specialChars, uuid);
            String decryptedSpecial = aesService.decrypt(encryptedSpecial, uuid);
            if (!specialChars.equals(decryptedSpecial)) {
                log.error("  测试5.5失败: 特殊字符解密结果不匹配");
                results.add(TestResult.failure("AES加密解密-特殊字符", "特殊字符解密结果不匹配", null));
            } else {
                log.debug("  测试5.5通过: 特殊字符加解密正常");
                results.add(TestResult.success("AES加密解密-特殊字符", "特殊字符加解密正常"));
            }

            // 5.6 测试JSON数据加密解密
            log.debug("  测试5.6: AES JSON数据加解密");
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("name", "测试用户");
            jsonData.put("age", 25);
            jsonData.put("email", "test@example.com");
            String jsonString = JSON.toJSONString(jsonData);
            String encryptedJson = aesService.encrypt(jsonString, uuid);
            String decryptedJson = aesService.decrypt(encryptedJson, uuid);
            @SuppressWarnings("unchecked")
            Map<String, Object> decryptedData = JSON.parseObject(decryptedJson, Map.class);
            if (!jsonData.equals(decryptedData)) {
                log.error("  测试5.6失败: JSON数据解密结果不匹配");
                results.add(TestResult.failure("AES加密解密-JSON", "JSON数据解密结果不匹配", null));
            } else {
                log.debug("  测试5.6通过: JSON数据加解密正常");
                results.add(TestResult.success("AES加密解密-JSON", "JSON数据加解密正常"));
            }

        } catch (Exception e) {
            log.error("AES加密解密测试异常", e);
            results.add(TestResult.failure("AES加密解密", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试6: RSA+AES组合流程
     */
    private List<TestResult> testRsaAesCombinedFlow() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.debug("开始RSA+AES组合流程测试，UUID: {}", uuid);

        try {
            // 6.1 完整流程：获取服务端公钥 -> 上传客户端公钥 -> 获取AES密钥 -> 使用AES加密数据
            // 步骤1: 获取服务端公钥
            log.debug("  测试6.1: 获取服务端公钥");
            rsaService.getKey(uuid);
            log.debug("  测试6.1通过: 成功获取服务端公钥");
            results.add(TestResult.success("组合流程-获取服务端公钥", "成功获取服务端公钥"));

            // 步骤2: 生成并上传客户端公钥
            log.debug("  测试6.2: 生成并上传客户端公钥");
            RsaKey clientKeyPair = RsaUtil.generate();
            String clientPublicKey = clientKeyPair.getPublicKey();
            String clientPrivateKey = clientKeyPair.getPrivateKey();
            rsaService.savePublicKey(uuid, clientPublicKey, null);
            log.debug("  测试6.2通过: 成功上传客户端公钥");
            results.add(TestResult.success("组合流程-上传客户端公钥", "成功上传客户端公钥"));

            // 步骤3: 获取AES密钥（使用客户端公钥RSA加密）
            log.debug("  测试6.3: 获取AES密钥（RSA加密传输）");
            AesKey aesKey = aesService.getKey(uuid);
            String encryptedAesKey = rsaService.encrypt(aesKey.getKey(), uuid);
            String encryptedAesVector = rsaService.encrypt(aesKey.getVector(), uuid);
            // 模拟客户端解密AES密钥
            String decryptedAesKey = RsaUtil.decrypt(encryptedAesKey, clientPrivateKey);
            String decryptedAesVector = RsaUtil.decrypt(encryptedAesVector, clientPrivateKey);
            if (!aesKey.getKey().equals(decryptedAesKey) || !aesKey.getVector().equals(decryptedAesVector)) {
                log.error("  测试6.3失败: AES密钥解密失败");
                results.add(TestResult.failure("组合流程-获取AES密钥", "AES密钥解密失败", null));
            } else {
                log.debug("  测试6.3通过: 成功获取并解密AES密钥");
                results.add(TestResult.success("组合流程-获取AES密钥", "成功获取并解密AES密钥"));
            }

            // 步骤4: 使用AES加密业务数据
            log.debug("  测试6.4: 使用AES加密业务数据");
            String businessData = "业务数据：用户登录信息";
            String encryptedBusinessData = AES.encrypt(businessData, decryptedAesKey, decryptedAesVector);
            // 模拟服务端使用AES解密
            String decryptedBusinessData = aesService.decrypt(encryptedBusinessData, uuid);
            if (!businessData.equals(decryptedBusinessData)) {
                log.error("  测试6.4失败: 业务数据解密失败");
                results.add(TestResult.failure("组合流程-AES加密业务数据", "业务数据解密失败", null));
            } else {
                log.debug("  测试6.4通过: 业务数据加解密正常");
                results.add(TestResult.success("组合流程-AES加密业务数据", "业务数据加解密正常"));
            }

            // 6.2 测试完整的数据传输流程（模拟真实场景）
            log.debug("  测试6.5: 完整数据传输流程（请求）");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("username", "testuser");
            requestData.put("password", "testpass123");
            requestData.put("timestamp", System.currentTimeMillis());
            String requestJson = JSON.toJSONString(requestData);

            // 客户端：使用AES加密请求数据
            String encryptedRequest = AES.encrypt(requestJson, decryptedAesKey, decryptedAesVector);
            // 服务端：使用AES解密请求数据
            String decryptedRequest = aesService.decrypt(encryptedRequest, uuid);
            @SuppressWarnings("unchecked")
            Map<String, Object> decryptedRequestData = JSON.parseObject(decryptedRequest, Map.class);
            if (!requestData.equals(decryptedRequestData)) {
                log.error("  测试6.5失败: 数据传输加解密失败");
                results.add(TestResult.failure("组合流程-数据传输", "数据传输加解密失败", null));
            } else {
                log.debug("  测试6.5通过: 数据传输加解密正常");
                results.add(TestResult.success("组合流程-数据传输", "数据传输加解密正常"));
            }

            // 服务端：准备响应数据
            log.debug("  测试6.6: 完整数据传输流程（响应）");
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("code", 0);
            responseData.put("msg", "success");
            responseData.put("data", "登录成功");
            String responseJson = JSON.toJSONString(responseData);

            // 服务端：使用AES加密响应数据
            String encryptedResponse = aesService.encrypt(responseJson, uuid);
            // 客户端：使用AES解密响应数据
            String decryptedResponse = AES.decrypt(encryptedResponse, decryptedAesKey, decryptedAesVector);
            @SuppressWarnings("unchecked")
            Map<String, Object> decryptedResponseData = JSON.parseObject(decryptedResponse, Map.class);
            if (!responseData.equals(decryptedResponseData)) {
                log.error("  测试6.6失败: 响应数据加解密失败");
                results.add(TestResult.failure("组合流程-响应数据", "响应数据加解密失败", null));
            } else {
                log.debug("  测试6.6通过: 响应数据加解密正常");
                results.add(TestResult.success("组合流程-响应数据", "响应数据加解密正常"));
            }

        } catch (Exception e) {
            log.error("RSA+AES组合流程测试异常", e);
            results.add(TestResult.failure("RSA+AES组合流程", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试7: 错误场景
     */
    private List<TestResult> testErrorScenarios() {
        List<TestResult> results = new ArrayList<>();
        log.debug("开始错误场景测试");

        try {
            // 7.1 测试UUID为空
            log.debug("  测试7.1: UUID为空-RSA");
            try {
                rsaService.getKey("");
                log.error("  测试7.1失败: 应该抛出异常但没有");
                results.add(TestResult.failure("错误场景-UUID为空-RSA", "应该抛出异常但没有", null));
            } catch (Exception e) {
                log.debug("  测试7.1通过: 正确抛出异常 - {}", e.getClass().getSimpleName());
                results.add(TestResult.success("错误场景-UUID为空-RSA", "正确抛出异常: " + e.getMessage()));
            }

            log.debug("  测试7.1: UUID为空-AES");
            try {
                aesService.getKey("");
                log.error("  测试7.1失败: 应该抛出异常但没有");
                results.add(TestResult.failure("错误场景-UUID为空-AES", "应该抛出异常但没有", null));
            } catch (Exception e) {
                log.debug("  测试7.1通过: 正确抛出异常 - {}", e.getClass().getSimpleName());
                results.add(TestResult.success("错误场景-UUID为空-AES", "正确抛出异常: " + e.getMessage()));
            }

            // 7.2 测试客户端公钥不存在时获取AES密钥
            log.debug("  测试7.2: 无客户端公钥生成AES密钥");
            String uuidForAes = UUID.randomUUID().toString();
            try {
                AesKey testAesKey = aesService.getKey(uuidForAes);
                // 应该成功，因为AES密钥生成不依赖客户端公钥
                if (testAesKey != null) {
                    log.debug("  测试7.2通过: AES密钥生成不依赖客户端公钥");
                    results.add(TestResult.success("错误场景-无客户端公钥生成AES密钥", "AES密钥生成不依赖客户端公钥"));
                } else {
                    log.error("  测试7.2失败: AES密钥生成返回null");
                    results.add(TestResult.failure("错误场景-无客户端公钥生成AES密钥", "AES密钥生成返回null", null));
                }
            } catch (Exception e) {
                log.error("  测试7.2失败: 不应该抛出异常", e);
                results.add(TestResult.failure("错误场景-无客户端公钥生成AES密钥", "不应该抛出异常", e));
            }

            // 7.3 测试使用不存在的UUID解密
            log.debug("  测试7.3: 不存在的UUID解密");
            String uuid2 = UUID.randomUUID().toString();
            try {
                aesService.decrypt("test", uuid2);
                log.error("  测试7.3失败: 应该抛出异常但没有");
                results.add(TestResult.failure("错误场景-不存在的UUID解密", "应该抛出异常但没有", null));
            } catch (CodeException e) {
                log.debug("  测试7.3通过: 正确抛出CodeException - {}", e.getMessage());
                results.add(TestResult.success("错误场景-不存在的UUID解密", "正确抛出CodeException: " + e.getMessage()));
            } catch (Exception e) {
                log.debug("  测试7.3通过: 正确抛出异常 - {}", e.getClass().getSimpleName());
                results.add(TestResult.success("错误场景-不存在的UUID解密", "正确抛出异常: " + e.getMessage()));
            }

            // 7.4 测试使用错误的加密数据解密
            log.debug("  测试7.4: 错误的加密数据解密");
            String uuid3 = UUID.randomUUID().toString();
            aesService.getKey(uuid3);
            String invalidCipher = Base64.getEncoder().encodeToString(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
            try {
                aesService.decrypt(invalidCipher, uuid3);
                log.error("  测试7.4失败: 应该抛出异常但没有");
                results.add(TestResult.failure("错误场景-错误的加密数据", "应该抛出异常但没有", null));
            } catch (Exception e) {
                log.debug("  测试7.4通过: 正确抛出异常 - {}", e.getClass().getSimpleName());
                results.add(TestResult.success("错误场景-错误的加密数据", "正确抛出异常: " + e.getMessage()));
            }

            // 7.5 测试使用错误的客户端公钥格式
            log.debug("  测试7.5: 错误的公钥格式");
            try {
                rsaService.savePublicKey(UUID.randomUUID().toString(), "invalid_public_key", null);
                log.error("  测试7.5失败: 应该抛出异常但没有");
                results.add(TestResult.failure("错误场景-错误的公钥格式", "应该抛出异常但没有", null));
            } catch (Exception e) {
                log.debug("  测试7.5通过: 正确抛出异常 - {}", e.getClass().getSimpleName());
                results.add(TestResult.success("错误场景-错误的公钥格式", "正确抛出异常: " + e.getMessage()));
            }

            // 7.6 测试空数据加密（当前实现返回空字符串，不抛错）
            log.debug("  测试7.6: 空数据加密");
            try {
                String uuidForEmpty = UUID.randomUUID().toString();
                aesService.getKey(uuidForEmpty);
                String encryptedEmpty = aesService.encrypt("", uuidForEmpty);
                if (!"".equals(encryptedEmpty)) {
                    log.error("  测试7.6失败: 空数据加密应返回空字符串");
                    results.add(TestResult.failure("错误场景-空数据加密", "空数据加密应返回空字符串", null));
                } else {
                    log.debug("  测试7.6通过: 空数据加密返回空字符串");
                    results.add(TestResult.success("错误场景-空数据加密", "空数据加密返回空字符串"));
                }
            } catch (Exception e) {
                log.error("  测试7.6失败: 空数据加密不应抛异常", e);
                results.add(TestResult.failure("错误场景-空数据加密", "空数据加密不应抛异常", e));
            }

        } catch (Exception e) {
            log.error("错误场景测试异常", e);
            results.add(TestResult.failure("错误场景", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试8: 边界情况
     */
    private List<TestResult> testBoundaryCases() {
        List<TestResult> results = new ArrayList<>();
        log.debug("开始边界情况测试");

        try {
            // 8.1 测试空字符串（当前实现返回空字符串，不抛错）
            log.debug("  测试8.1: 空字符串加密");
            String uuid = UUID.randomUUID().toString();
            AesKey aesKey = aesService.getKey(uuid);
            if (aesKey == null) {
                log.error("  测试8.1失败: 无法生成AES密钥");
                results.add(TestResult.failure("边界情况-空字符串", "无法生成AES密钥", null));
            } else {
                String encryptedEmpty = aesService.encrypt("", uuid);
                if (!"".equals(encryptedEmpty)) {
                    log.error("  测试8.1失败: 空字符串加密应返回空字符串");
                    results.add(TestResult.failure("边界情况-空字符串", "空字符串加密应返回空字符串", null));
                } else {
                    log.debug("  测试8.1通过: 空字符串加密返回空字符串");
                    results.add(TestResult.success("边界情况-空字符串", "空字符串加密返回空字符串"));
                }
            }

            // 8.2 测试单个字符
            log.debug("  测试8.2: 单个字符加解密");
            String uuid2 = UUID.randomUUID().toString();
            AesKey aesKey2 = aesService.getKey(uuid2);
            if (aesKey2 == null) {
                log.error("  测试8.2失败: 无法生成AES密钥");
                results.add(TestResult.failure("边界情况-单个字符", "无法生成AES密钥", null));
            } else {
                String singleChar = "A";
                try {
                    String encryptedSingle = aesService.encrypt(singleChar, uuid2);
                    String decryptedSingle = aesService.decrypt(encryptedSingle, uuid2);
                    if (!singleChar.equals(decryptedSingle)) {
                        log.error("  测试8.2失败: 单个字符加解密失败");
                        results.add(TestResult.failure("边界情况-单个字符", "单个字符加解密失败", null));
                    } else {
                        log.debug("  测试8.2通过: 单个字符加解密正常");
                        results.add(TestResult.success("边界情况-单个字符", "单个字符加解密正常"));
                    }
                } catch (Exception e) {
                    log.error("  测试8.2失败: 加解密过程异常", e);
                    results.add(TestResult.failure("边界情况-单个字符", "加解密过程异常", e));
                }
            }

            // 8.3 测试Unicode字符
            log.debug("  测试8.3: Unicode字符加解密");
            String uuid3 = UUID.randomUUID().toString();
            AesKey aesKey3 = aesService.getKey(uuid3);
            if (aesKey3 == null) {
                log.error("  测试8.3失败: 无法生成AES密钥");
                results.add(TestResult.failure("边界情况-Unicode字符", "无法生成AES密钥", null));
            } else {
                String unicode = "测试中文 🎉 émoji 特殊字符";
                try {
                    String encryptedUnicode = aesService.encrypt(unicode, uuid3);
                    String decryptedUnicode = aesService.decrypt(encryptedUnicode, uuid3);
                    if (!unicode.equals(decryptedUnicode)) {
                        log.error("  测试8.3失败: Unicode字符加解密失败");
                        results.add(TestResult.failure("边界情况-Unicode字符", "Unicode字符加解密失败", null));
                    } else {
                        log.debug("  测试8.3通过: Unicode字符加解密正常");
                        results.add(TestResult.success("边界情况-Unicode字符", "Unicode字符加解密正常"));
                    }
                } catch (Exception e) {
                    log.error("  测试8.3失败: 加解密过程异常", e);
                    results.add(TestResult.failure("边界情况-Unicode字符", "加解密过程异常", e));
                }
            }

            // 8.4 测试大量并发请求（模拟）
            log.debug("  测试8.4: 并发请求测试（10个请求）");
            String uuid4 = UUID.randomUUID().toString();
            AesKey aesKey4 = aesService.getKey(uuid4);
            if (aesKey4 == null) {
                log.error("  测试8.4失败: 无法生成AES密钥");
                results.add(TestResult.failure("边界情况-并发请求", "无法生成AES密钥", null));
            } else {
                int successCount = 0;
                for (int i = 0; i < 10; i++) {
                    try {
                        String testData = "并发测试数据 " + i;
                        String encrypted = aesService.encrypt(testData, uuid4);
                        String decrypted = aesService.decrypt(encrypted, uuid4);
                        if (testData.equals(decrypted)) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.debug("  并发请求 {} 失败: {}", i, e.getMessage());
                        // 忽略单个失败
                    }
                }
                if (successCount == 10) {
                    log.debug("  测试8.4通过: 10个并发请求全部成功");
                    results.add(TestResult.success("边界情况-并发请求", "10个并发请求全部成功"));
                } else {
                    log.warn("  测试8.4失败: 10个并发请求中只有{}个成功", successCount);
                    results.add(TestResult.failure("边界情况-并发请求", "10个并发请求中只有" + successCount + "个成功", null));
                }
            }

            // 8.5 测试密钥过期（模拟）
            log.debug("  测试8.5: 密钥过期检查");
            // 注意：实际测试中需要等待密钥过期，这里只测试过期检查逻辑
            String uuid5 = UUID.randomUUID().toString();
            AesKey aesKey5 = aesService.getKey(uuid5);
            if (aesKey5 == null) {
                log.error("  测试8.5失败: 无法生成AES密钥");
                results.add(TestResult.failure("边界情况-密钥过期检查", "无法生成AES密钥", null));
            } else if (aesKey5.expired()) {
                log.error("  测试8.5失败: 新生成的密钥不应该过期");
                results.add(TestResult.failure("边界情况-密钥过期检查", "新生成的密钥不应该过期", null));
            } else {
                log.debug("  测试8.5通过: 密钥过期检查正常");
                results.add(TestResult.success("边界情况-密钥过期检查", "密钥过期检查正常"));
            }

        } catch (Exception e) {
            log.error("边界情况测试异常", e);
            results.add(TestResult.failure("边界情况", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 测试AES加密解密超长字符串的性能
     * 测试不同长度的字符串，记录加密和解密时间，计算吞吐量
     *
     * @return 测试结果列表
     */
    public List<TestResult> testAesLongStringPerformance() {
        List<TestResult> results = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        log.info(repeat("=", 80));
        log.info("开始AES超长字符串性能测试");
        log.info(repeat("=", 80));

        try {
            // 生成AES密钥
            AesKey aesKey = aesService.getKey(uuid);
            String aesKeyBase64 = aesKey.getKey();
            String aesVectorBase64 = aesKey.getVector();
            log.info("已生成AES密钥，UUID: {}", uuid);

            // 定义测试的数据大小（字节）
            int[] testSizes = {
                    1024,           // 1KB
                    10 * 1024,       // 10KB
                    100 * 1024,      // 100KB
                    1024 * 1024,     // 1MB
                    10 * 1024 * 1024 // 10MB
            };

            String[] sizeNames = {"1KB", "10KB", "100KB", "1MB", "10MB"};

            // 生成测试数据模板
            String template = "这是一个用于AES性能测试的字符串模板，包含中文字符和英文字母：ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder templateBuilder = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                templateBuilder.append(template);
            }
            String baseTemplate = templateBuilder.toString();

            log.info("\n测试配置:");
            log.info("  密钥长度: {} 位", aesKeyBase64.length() * 6 / 8); // Base64编码，约6位表示8位
            log.info("  向量长度: {} 字节", aesVectorBase64.length() * 3 / 4); // Base64编码
            log.info("  测试次数: {} 种不同大小", testSizes.length);
            log.info(repeat("-", 80));

            for (int i = 0; i < testSizes.length; i++) {
                int targetSize = testSizes[i];
                String sizeName = sizeNames[i];
                log.info("\n【测试 {}】数据大小: {} ({})", i + 1, sizeName, formatBytes(targetSize));

                try {
                    // 生成指定大小的测试数据
                    StringBuilder testDataBuilder = new StringBuilder();
                    while (testDataBuilder.length() < targetSize) {
                        testDataBuilder.append(baseTemplate);
                    }
                    String testData = testDataBuilder.substring(0, targetSize);
                    int actualSize = testData.getBytes("UTF-8").length;
                    log.info("  实际数据大小: {} ({} 字节)", formatBytes(actualSize), actualSize);

                    // 预热：执行一次加密解密，避免JVM预热影响
                    aesService.encrypt(testData.substring(0, Math.min(1000, testData.length())), uuid);
                    aesService.decrypt(aesService.encrypt(testData.substring(0, Math.min(1000, testData.length())), uuid), uuid);

                    // 测试加密性能
                    int encryptRounds = 5; // 执行5次取平均值
                    long totalEncryptTime = 0;
                    String encrypted = null;

                    for (int round = 0; round < encryptRounds; round++) {
                        long encryptStart = System.nanoTime();
                        String encryptedRound = aesService.encrypt(testData, uuid);
                        long encryptEnd = System.nanoTime();
                        totalEncryptTime += (encryptEnd - encryptStart);
                        if (round == encryptRounds - 1) {
                            encrypted = encryptedRound; // 保存最后一次加密结果用于解密测试
                        }
                    }

                    if (encrypted == null) {
                        log.error("  加密失败: 无法获取加密结果");
                        results.add(TestResult.failure(
                                "AES性能测试-" + sizeName,
                                String.format("加密失败，大小: %s", sizeName),
                                null
                        ).addData("size", sizeName));
                        continue;
                    }

                    // 此时 encrypted 一定不为 null
                    final String finalEncrypted = encrypted;
                    int encryptedSizeBytes = finalEncrypted.length();

                    long avgEncryptTimeNs = totalEncryptTime / encryptRounds;
                    double avgEncryptTimeMs = avgEncryptTimeNs / 1_000_000.0;
                    double encryptThroughputMBps = (actualSize / (1024.0 * 1024.0)) / (avgEncryptTimeMs / 1000.0);

                    log.info("  加密性能:");
                    log.info("    平均耗时: {} ms", String.format("%.2f", avgEncryptTimeMs));
                    log.info("    吞吐量: {} MB/s", String.format("%.2f", encryptThroughputMBps));
                    log.info("    加密后大小: {} ({} 字节)", formatBytes(encryptedSizeBytes), encryptedSizeBytes);

                    // 测试解密性能
                    long totalDecryptTime = 0;
                    String decrypted = null;

                    for (int round = 0; round < encryptRounds; round++) {
                        long decryptStart = System.nanoTime();
                        decrypted = aesService.decrypt(finalEncrypted, uuid);
                        long decryptEnd = System.nanoTime();
                        totalDecryptTime += (decryptEnd - decryptStart);
                    }

                    if (decrypted == null) {
                        log.error("  解密失败: 无法获取解密结果");
                        results.add(TestResult.failure(
                                "AES性能测试-" + sizeName,
                                String.format("解密失败，大小: %s", sizeName),
                                null
                        ).addData("size", sizeName)
                                .addData("encryptTimeMs", avgEncryptTimeMs)
                                .addData("encryptThroughputMBps", encryptThroughputMBps));
                        continue;
                    }

                    long avgDecryptTimeNs = totalDecryptTime / encryptRounds;
                    double avgDecryptTimeMs = avgDecryptTimeNs / 1_000_000.0;
                    double decryptThroughputMBps = (actualSize / (1024.0 * 1024.0)) / (avgDecryptTimeMs / 1000.0);

                    log.info("  解密性能:");
                    log.info("    平均耗时: {} ms", String.format("%.2f", avgDecryptTimeMs));
                    log.info("    吞吐量: {} MB/s", String.format("%.2f", decryptThroughputMBps));

                    // 验证数据完整性
                    boolean dataMatch = testData.equals(decrypted);
                    if (!dataMatch) {
                        log.error("  数据验证失败: 解密后的数据与原始数据不匹配");
                        results.add(TestResult.failure(
                                "AES性能测试-" + sizeName,
                                String.format("数据验证失败，大小: %s", sizeName),
                                null
                        ).addData("size", sizeName)
                                .addData("encryptTimeMs", avgEncryptTimeMs)
                                .addData("decryptTimeMs", avgDecryptTimeMs)
                                .addData("encryptThroughputMBps", encryptThroughputMBps)
                                .addData("decryptThroughputMBps", decryptThroughputMBps));
                    } else {
                        log.info("  数据验证: ✓ 通过");
                        results.add(TestResult.success(
                                "AES性能测试-" + sizeName,
                                String.format("加密: %.2f ms (%.2f MB/s), 解密: %.2f ms (%.2f MB/s)",
                                        avgEncryptTimeMs, encryptThroughputMBps,
                                        avgDecryptTimeMs, decryptThroughputMBps)
                        ).addData("size", sizeName)
                                .addData("dataSizeBytes", actualSize)
                                .addData("encryptTimeMs", avgEncryptTimeMs)
                                .addData("decryptTimeMs", avgDecryptTimeMs)
                                .addData("encryptThroughputMBps", encryptThroughputMBps)
                                .addData("decryptThroughputMBps", decryptThroughputMBps)
                                .addData("encryptedSizeBytes", encryptedSizeBytes));
                    }

                } catch (Exception e) {
                    log.error("  测试失败: {}", e.getMessage(), e);
                    results.add(TestResult.failure(
                            "AES性能测试-" + sizeName,
                            String.format("测试过程中发生异常: %s", e.getMessage()),
                            e
                    ).addData("size", sizeName));
                }
            }

            // 输出性能总结
            log.info("\n" + repeat("=", 80));
            log.info("性能测试总结:");
            log.info(repeat("-", 80));
            for (TestResult result : results) {
                if (result.isSuccess()) {
                    Map<String, Object> data = result.getData();
                    String size = (String) data.get("size");
                    Double encryptTime = (Double) data.get("encryptTimeMs");
                    Double decryptTime = (Double) data.get("decryptTimeMs");
                    Double encryptThroughput = (Double) data.get("encryptThroughputMBps");
                    Double decryptThroughput = (Double) data.get("decryptThroughputMBps");
                    log.info("  {}: 加密 {} ms ({} MB/s), 解密 {} ms ({} MB/s)",
                            size,
                            String.format("%.2f", encryptTime),
                            String.format("%.2f", encryptThroughput),
                            String.format("%.2f", decryptTime),
                            String.format("%.2f", decryptThroughput));
                }
            }
            log.info(repeat("=", 80));

        } catch (Exception e) {
            log.error("AES超长字符串性能测试异常", e);
            results.add(TestResult.failure("AES性能测试", "测试过程中发生异常", e));
        }

        return results;
    }

    /**
     * 格式化字节数为可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * 生成重复字符串（兼容Java 8）
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 生成测试报告
     *
     * @param results 测试结果列表
     * @return 测试报告字符串
     */
    public String generateTestReport(List<TestResult> results) {
        StringBuilder report = new StringBuilder();
        report.append(repeat("=", 80)).append("\n");
        report.append("RSA和AES组合加密方案测试报告\n");
        report.append(repeat("=", 80)).append("\n");
        report.append("测试时间: ").append(new Date()).append("\n");
        report.append("总测试数: ").append(results.size()).append("\n");

        long successCount = results.stream().filter(TestResult::isSuccess).count();
        long failureCount = results.size() - successCount;
        report.append("成功: ").append(successCount).append("\n");
        report.append("失败: ").append(failureCount).append("\n");
        report.append("成功率: ").append(String.format("%.2f%%", (successCount * 100.0 / results.size()))).append("\n");
        report.append(repeat("-", 80)).append("\n");

        // 按测试名称分组
        Map<String, List<TestResult>> groupedResults = new LinkedHashMap<>();
        for (TestResult result : results) {
            String groupName = result.getTestName().split("-")[0];
            groupedResults.computeIfAbsent(groupName, k -> new ArrayList<>()).add(result);
        }

        // 输出详细结果
        for (Map.Entry<String, List<TestResult>> entry : groupedResults.entrySet()) {
            report.append("\n【").append(entry.getKey()).append("】\n");
            for (TestResult result : entry.getValue()) {
                report.append(result.isSuccess() ? "✓ " : "✗ ");
                report.append(result.getTestName()).append(": ");
                report.append(result.getMessage());
                if (result.getException() != null) {
                    report.append(" (").append(result.getException().getClass().getSimpleName()).append(")");
                }
                report.append("\n");
            }
        }

        report.append("\n").append(repeat("=", 80)).append("\n");
        return report.toString();
    }
}
