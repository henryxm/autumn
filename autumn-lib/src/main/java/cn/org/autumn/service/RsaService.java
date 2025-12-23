package cn.org.autumn.service;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.*;
import cn.org.autumn.model.Error;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.utils.RsaUtil;
import cn.org.autumn.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RsaService {

    @Autowired
    CacheService cacheService;

    @Autowired
    EncryptConfigFactory encryptConfigFactory;

    /**
     * 支持加密的类型接口列表
     */
    static List<EndpointInfo> result = null;

    /**
     * 服务端密钥对缓存配置
     * 缓存过期时间 = 密钥对有效期 + 服务端冗余保留时间
     * 确保在密钥对过期后，服务端仍能解密正在传输的数据
     */
    private static CacheConfig serverPrivateKeyConfig;

    /**
     * 客户端公钥缓存配置
     */
    private static CacheConfig clientPublicKeyConfig;

    public CacheConfig getServerPrivateKeyConfig() {
        if (null == serverPrivateKeyConfig) {
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            serverPrivateKeyConfig = CacheConfig.builder()
                    .cacheName("RsaServiceCache")
                    .keyType(String.class)
                    .valueType(RsaKey.class)
                    .expireTime(config.getKeyValidMinutes() + config.getServerBufferMinutes())
                    .timeUnit(TimeUnit.MINUTES)
                    .build();
        }
        return serverPrivateKeyConfig;
    }

    public CacheConfig getClientPublicKeyConfig() {
        if (null == clientPublicKeyConfig) {
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            clientPublicKeyConfig = CacheConfig.builder()
                    .cacheName("ClientPublicKeyCache")
                    .keyType(String.class)
                    .valueType(RsaKey.class)
                    .expireTime(config.getClientPublicKeyValidMinutes())
                    .timeUnit(TimeUnit.MINUTES)
                    .build();
        }
        return clientPublicKeyConfig;
    }

    /**
     * 生成新的密钥对
     *
     * @param uuid 客户端UUID标识
     * @return 包含过期时间的密钥对
     */
    public RsaKey generate(String uuid) {
        try {
            if (StringUtils.isBlank(uuid)) {
                throw new RuntimeException(new CodeException(Error.RSA_UUID_REQUIRED));
            }
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            RsaKey pair = RsaUtil.generate(config.getKeySize());
            pair.setSession(uuid);
            // 设置过期时间：当前时间 + 密钥对有效期
            long expireTime = System.currentTimeMillis() + (config.getKeyValidMinutes() * 60 * 1000L);
            pair.setExpireTime(expireTime);
            if (log.isDebugEnabled()) {
                log.debug("生成新的密钥对，UUID: {}, 密钥长度: {}位, 过期时间: {}", uuid, config.getKeySize(), expireTime);
            }
            return pair;
        } catch (Exception e) {
            log.error("生成RSA密钥对失败，UUID: {}", uuid, e);
            throw new RuntimeException(new CodeException(Error.RSA_KEY_GENERATE_FAILED));
        }
    }

    /**
     * 获取密钥对（如果不存在则生成新的）
     * 返回的密钥对包含过期时间，客户端应在此时间之前重新获取
     * 仅在客户端主动请求获取密钥时调用，如果密钥即将过期则生成新的
     *
     * @param uuid 客户端UUID标识
     * @return 包含过期时间的密钥对
     * @throws CodeException 密钥获取或生成失败时抛出异常
     */
    public RsaKey getRsaKey(String uuid) throws CodeException {
        try {
            if (StringUtils.isBlank(uuid)) {
                throw new CodeException(Error.RSA_UUID_REQUIRED);
            }
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            // 使用compute方法：如果缓存不存在则生成，存在则返回
            RsaKey pair = cacheService.compute(uuid, () -> generate(uuid), getServerPrivateKeyConfig());
            // 检查密钥对是否有效：为null、已过期、格式无效或即将过期时，删除缓存并重新调用compute
            // 利用短路求值：如果pair为null，后面的条件不会执行
            if (pair == null || pair.isExpired() || StringUtils.isBlank(pair.getPublicKey()) || StringUtils.isBlank(pair.getPrivateKey()) || pair.isExpiringSoon(config.getClientBufferMinutes())) {
                // 删除缓存并重新调用compute
                cacheService.remove(getServerPrivateKeyConfig().getCacheName(), uuid);
                pair = cacheService.compute(uuid, () -> generate(uuid), getServerPrivateKeyConfig());
            }
            return pair;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CodeException) {
                throw (CodeException) e.getCause();
            }
            log.error("获取密钥对失败，UUID: {}", uuid, e);
            throw new CodeException(Error.RSA_KEY_PAIR_NOT_FOUND);
        } catch (Exception e) {
            log.error("获取密钥对失败，UUID: {}", uuid, e);
            throw new CodeException(Error.RSA_KEY_PAIR_NOT_FOUND);
        }
    }

    /**
     * 解密数据（使用UUID）
     * 支持旧密钥对的平滑切换：即使密钥对已过期，只要在服务端冗余保留时间内，仍可解密
     *
     * @param value 加密数据（包含uuid）
     * @return 解密后的数据
     * @throws CodeException 解密失败时抛出异常
     */
    public String decrypt(Encrypt value) throws CodeException {
        if (StringUtils.isBlank(value.getCiphertext())) {
            return "";
        }
        if (StringUtils.isBlank(value.getSession())) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        // 从缓存中获取密钥对
        RsaKey keyPair = cacheService.get(getServerPrivateKeyConfig().getCacheName(), value.getSession());
        if (keyPair == null) {
            throw new CodeException(Error.RSA_PRIVATE_KEY_NOT_FOUND);
        }
        String privateKey = keyPair.getPrivateKey();
        if (StringUtils.isBlank(privateKey)) {
            throw new CodeException(Error.RSA_PRIVATE_KEY_NOT_FOUND);
        }
        // 检查密钥对是否已过期（但仍在服务端冗余保留时间内）
        if (keyPair.isExpired()) {
            log.warn("使用已过期的密钥对进行解密，UUID: {}, 过期时间: {}", value.getSession(), keyPair.getExpireTime());
        }
        // 执行解密
        try {
            String decrypted = RsaUtil.decrypt(value.getCiphertext(), privateKey);
            if (StringUtils.isBlank(decrypted)) {
                log.warn("解密结果为空，UUID: {}", value.getSession());
            }
            return decrypted;
        } catch (IllegalArgumentException e) {
            log.error("RSA密钥格式错误，解密失败，UUID: {}, 错误: {}", value.getSession(), e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("RSA解密失败，UUID: {}, 错误: {}", value.getSession(), e.getMessage(), e);
            throw new CodeException(Error.RSA_DECRYPT_FAILED);
        }
    }

    /**
     * 保存客户端公钥（使用UUID作为客户端标识）
     *
     * @param uuid       客户端UUID标识
     * @param publicKey  客户端公钥
     * @param expireTime 客户端指定的过期时间（毫秒时间戳），如果为null则使用后端默认过期时间
     * @return 保存的客户端公钥信息
     * @throws CodeException 保存失败时抛出异常
     */
    public RsaKey savePublicKey(String uuid, String publicKey, Long expireTime) throws CodeException {
        if (StringUtils.isBlank(uuid)) {
            throw new CodeException(Error.RSA_UUID_REQUIRED);
        }
        if (StringUtils.isBlank(publicKey)) {
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        }
        // 验证公钥格式：尝试使用公钥加密一个测试字符串来验证格式
        try {
            String testData = "test";
            RsaUtil.encrypt(testData, publicKey);
        } catch (IllegalArgumentException e) {
            log.error("客户端公钥格式错误，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (RuntimeException e) {
            // RsaUtil.encrypt可能抛出RuntimeException包装的InvalidKeyException
            Throwable cause = e.getCause();
            if (cause instanceof java.security.InvalidKeyException) {
                log.error("客户端公钥解析失败，UUID: {}, 错误: {}", uuid, cause.getMessage());
                throw new CodeException(Error.RSA_PUBLIC_KEY_PARSE_ERROR);
            }
            log.error("客户端公钥验证失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("客户端公钥验证失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        }
        // 处理过期时间
        EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
        long finalExpireTime;
        long currentTime = System.currentTimeMillis();
        long maxExpireTime = currentTime + (config.getClientPublicKeyValidMinutes() * 60 * 1000L); // 最大过期时间（后端默认）
        if (expireTime != null) {
            // 客户端提供了过期时间，进行验证
            if (expireTime <= currentTime) {
                throw new IllegalArgumentException("过期时间不能小于或等于当前时间");
            }
            if (expireTime > maxExpireTime) {
                log.warn("客户端指定的过期时间超过最大有效期，使用最大有效期。UUID: {}, 客户端指定: {}, 最大有效期: {}", uuid, expireTime, maxExpireTime);
                finalExpireTime = maxExpireTime;
            } else {
                finalExpireTime = expireTime;
                if (log.isDebugEnabled()) {
                    log.debug("使用客户端指定的过期时间，UUID: {}, 过期时间: {}", uuid, finalExpireTime);
                }
            }
        } else {
            // 客户端未提供过期时间，使用后端默认过期时间
            finalExpireTime = maxExpireTime;
            if (log.isDebugEnabled()) {
                log.debug("使用后端默认过期时间，UUID: {}, 过期时间: {}", uuid, finalExpireTime);
            }
        }
        // 创建客户端公钥对象
        RsaKey clientPublicKey = new RsaKey(uuid, publicKey, finalExpireTime);
        // 保存到缓存
        cacheService.put(getClientPublicKeyConfig().getCacheName(), uuid, clientPublicKey);
        if (log.isDebugEnabled()) {
            log.debug("保存客户端公钥，UUID: {}, 过期时间: {}", uuid, finalExpireTime);
        }
        return clientPublicKey;
    }

    /**
     * 获取客户端公钥
     *
     * @param uuid 客户端标识
     * @return 客户端公钥信息，如果不存在或已过期返回null
     */
    public RsaKey getClientPublicKey(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        RsaKey clientPublicKey = cacheService.get(getClientPublicKeyConfig().getCacheName(), uuid);
        if (clientPublicKey != null && clientPublicKey.isExpired()) {
            log.warn("客户端公钥已过期，ClientId: {}, 过期时间: {}", uuid, clientPublicKey.getExpireTime());
            // 可以选择删除过期公钥或返回null
            return null;
        }
        return clientPublicKey;
    }

    /**
     * 使用客户端公钥加密数据（使用UUID）
     * 服务端使用客户端的公钥加密数据，返回给客户端，客户端使用自己的私钥解密
     *
     * @param data 待加密的数据
     * @param uuid 客户端UUID标识
     * @return Base64编码的加密数据
     * @throws CodeException 加密失败时抛出异常
     */
    public String encrypt(String data, String uuid) throws CodeException {
        if (StringUtils.isBlank(data)) {
            throw new IllegalArgumentException("待加密数据不能为空");
        }
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("UUID不能为空");
        }
        // 获取客户端公钥
        RsaKey clientPublicKey = getClientPublicKey(uuid);
        if (clientPublicKey == null) {
            throw new CodeException(Error.RSA_CLIENT_PUBLIC_KEY_NOT_FOUND);
        }
        // 检查公钥是否即将过期
        EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
        if (clientPublicKey.isExpiringSoon(config.getClientBufferMinutes())) {
            log.warn("客户端公钥即将过期，建议客户端更新公钥，UUID: {}, 过期时间: {}", uuid, clientPublicKey.getExpireTime());
        }
        try {
            // 使用客户端公钥加密
            String encrypted = RsaUtil.encrypt(data, clientPublicKey.getPublicKey());
            if (StringUtils.isBlank(encrypted)) {
                log.warn("加密结果为空，UUID: {}", uuid);
                throw new CodeException(Error.RSA_ENCRYPT_FAILED);
            }
            return encrypted;
        } catch (IllegalArgumentException e) {
            log.error("RSA密钥格式错误，加密失败，UUID: {}, 错误: {}", uuid, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("使用客户端公钥加密失败，UUID: {}, 错误: {}", uuid, e.getMessage(), e);
            throw new CodeException(Error.RSA_ENCRYPT_FAILED);
        }
    }

    /**
     * 检查客户端公钥是否存在且有效
     *
     * @param uuid 客户端标识
     * @return true表示存在且有效，false表示不存在或已过期
     */
    public boolean hasValidClientPublicKey(String uuid) {
        RsaKey clientPublicKey = getClientPublicKey(uuid);
        return clientPublicKey != null && !clientPublicKey.isExpired();
    }

    public List<EndpointInfo> getEncryptEndpoints() throws CodeException {
        if (null != result)
            return result;
        ApplicationContext context = SpringContextUtils.getApplicationContext();
        if (context == null) {
            throw new CodeException(Error.INTERNAL_SERVER_ERROR);
        }
        result = new ArrayList<>();
        // 获取所有RestController类型的Bean
        Map<String, Object> restControllers = context.getBeansWithAnnotation(RestController.class);
        for (Map.Entry<String, Object> entry : restControllers.entrySet()) {
            Object controller = entry.getValue();
            Class<?> controllerClass = controller.getClass();
            // 检查类级别的EncryptEndpoint注解
            Endpoint classEndpoint = controllerClass.getAnnotation(Endpoint.class);
            boolean classHidden = classEndpoint != null && classEndpoint.hidden();
            // 如果类级别标记为隐藏，则跳过整个类
            if (classHidden) {
                continue;
            }
            // 获取类级别的RequestMapping注解
            RequestMapping classMapping = controllerClass.getAnnotation(RequestMapping.class);
            String basePath = "";
            if (classMapping != null && classMapping.value().length > 0) {
                basePath = classMapping.value()[0];
            }
            // 扫描所有方法
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                // 检查方法是否有请求映射注解
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                PutMapping putMapping = method.getAnnotation(PutMapping.class);
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
                if (requestMapping == null && getMapping == null && postMapping == null && putMapping == null && deleteMapping == null && patchMapping == null) {
                    continue; // 跳过没有请求映射注解的方法
                }
                // 检查方法级别的隐藏标记（方法级别优先级高于类级别）
                Endpoint methodEndpoint = method.getAnnotation(Endpoint.class);
                boolean methodHidden = methodEndpoint != null && methodEndpoint.hidden();
                // 如果方法级别标记为隐藏，则跳过该方法
                if (methodHidden) {
                    continue;
                }
                // 获取请求路径
                String methodPath = "";
                String httpMethod = "";
                if (requestMapping != null) {
                    if (requestMapping.value().length > 0) {
                        methodPath = requestMapping.value()[0];
                    }
                    if (requestMapping.method().length > 0) {
                        httpMethod = requestMapping.method()[0].name();
                    }
                } else if (getMapping != null) {
                    if (getMapping.value().length > 0) {
                        methodPath = getMapping.value()[0];
                    }
                    httpMethod = "GET";
                } else if (postMapping != null) {
                    if (postMapping.value().length > 0) {
                        methodPath = postMapping.value()[0];
                    }
                    httpMethod = "POST";
                } else if (putMapping != null) {
                    if (putMapping.value().length > 0) {
                        methodPath = putMapping.value()[0];
                    }
                    httpMethod = "PUT";
                } else if (deleteMapping != null) {
                    if (deleteMapping.value().length > 0) {
                        methodPath = deleteMapping.value()[0];
                    }
                    httpMethod = "DELETE";
                } else {
                    if (patchMapping.value().length > 0) {
                        methodPath = patchMapping.value()[0];
                    }
                    httpMethod = "PATCH";
                }
                // 构建完整路径
                String fullPath = basePath + methodPath;
                // 检查请求body参数类型
                Parameter[] parameters = method.getParameters();
                List<EncryptRequestBodyType> encryptRequestBodyTypes = new ArrayList<>();
                for (Parameter parameter : parameters) {
                    RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
                    if (requestBody != null) {
                        Class<?> paramType = parameter.getType();
                        if (isEncryptType(paramType)) {
                            EncryptRequestBodyType bodyInfo = new EncryptRequestBodyType();
                            bodyInfo.setParameterName(parameter.getName());
                            bodyInfo.setType(paramType.getName());
                            bodyInfo.setSimpleType(paramType.getSimpleName());
                            encryptRequestBodyTypes.add(bodyInfo);
                        }
                    }
                }
                // 检查返回类型
                Class<?> returnType = method.getReturnType();
                boolean isEncryptReturnType = isEncryptType(returnType);
                // 如果请求body或返回值类型是Encrypt，则添加到结果中
                if (!encryptRequestBodyTypes.isEmpty() || isEncryptReturnType) {
                    EndpointInfo endpointInfo = new EndpointInfo();
                    endpointInfo.setPath(fullPath);
                    endpointInfo.setMethod(httpMethod);
                    endpointInfo.setEncryptBody(!encryptRequestBodyTypes.isEmpty());
                    endpointInfo.setEncryptReturn(isEncryptReturnType);
                    result.add(endpointInfo);
                }
            }
        }
        return result;
    }

    /**
     * 判断类型是否实现了Encrypt接口
     *
     * @param type 要检查的类型
     * @return 如果类型实现了Encrypt接口返回true，否则返回false
     */
    private boolean isEncryptType(Class<?> type) {
        if (type == null) {
            return false;
        }
        // 检查是否直接实现了Encrypt接口
        if (Encrypt.class.isAssignableFrom(type)) {
            return true;
        }
        // 检查所有实现的接口
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> iface : interfaces) {
            if (Encrypt.class.isAssignableFrom(iface)) {
                return true;
            }
        }
        // 检查父类
        Class<?> superClass = type.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return isEncryptType(superClass);
        }
        return false;
    }
}
