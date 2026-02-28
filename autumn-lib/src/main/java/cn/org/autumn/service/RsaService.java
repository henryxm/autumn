package cn.org.autumn.service;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.model.Supported;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.config.EncryptionLoader;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.*;
import cn.org.autumn.model.Error;
import cn.org.autumn.site.EncryptConfigFactory;
import cn.org.autumn.site.LoadFactory;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RsaService implements LoadFactory.Must {

    @Autowired
    CacheService cacheService;

    @Autowired
    EncryptConfigFactory encryptConfigFactory;

    @Autowired(required = false)
    EncryptionLoader encryptionLoader;

    /**
     * 支持加密的类型接口列表
     */
    static List<EndpointInfo> result = null;

    /**
     * 服务端密钥对缓存配置
     * 缓存过期时间 = 密钥对有效期 + 服务端冗余保留时间
     * 确保在密钥对过期后，服务端仍能解密正在传输的数据
     */
    private static CacheConfig server;

    /**
     * 客户端公钥缓存配置
     */
    private static CacheConfig client;

    public CacheConfig getServerConfig() {
        if (null == server) {
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            server = CacheConfig.builder().name("rsaservice").key(String.class).value(RsaKey.class).expire(config.getKeyValidMinutes()).redis(config.getKeyValidMinutes() + config.getServerBufferMinutes()).unit(TimeUnit.MINUTES).build();
        }
        return server;
    }

    public CacheConfig getClientConfig() {
        if (null == client) {
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            client = CacheConfig.builder().name("clientkey").key(String.class).value(RsaKey.class).expire(config.getClientPublicKeyValidMinutes()).redis(config.getClientPublicKeyValidMinutes() + config.getServerBufferMinutes()).unit(TimeUnit.MINUTES).build();
        }
        return client;
    }

    /**
     * 生成新的密钥对
     *
     * @param session 客户端UUID标识
     * @return 包含过期时间的密钥对
     */
    public RsaKey generate(String session) {
        try {
            if (StringUtils.isBlank(session)) {
                throw new RuntimeException(new CodeException(Error.RSA_SESSION_REQUIRED));
            }
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            RsaKey pair = RsaUtil.generate(config.getKeySize());
            pair.setSession(session);
            // 设置过期时间：当前时间 + 密钥对有效期
            long expireTime = System.currentTimeMillis() + (config.getKeyValidMinutes() * 60 * 1000L);
            pair.setExpireTime(expireTime);
            if (log.isDebugEnabled()) {
                log.debug("生成新的密钥对，Session: {}, 密钥长度: {}位, 过期时间: {}", session, config.getKeySize(), expireTime);
            }
            return pair;
        } catch (Exception e) {
            log.error("生成RSA密钥对失败，Session: {}", session, e);
            throw new RuntimeException(new CodeException(Error.RSA_KEY_GENERATE_FAILED));
        }
    }

    private RsaKey load(String session) {
        // 如果缓存中没有，尝试从数据库加载
        if (encryptionLoader != null) {
            RsaKey loadedKey = encryptionLoader.loadRsa(session);
            if (loadedKey != null && !loadedKey.expired() && StringUtils.isNotBlank(loadedKey.getPublicKey()) && StringUtils.isNotBlank(loadedKey.getPrivateKey())) {
                if (log.isDebugEnabled()) {
                    log.debug("数据库加载:{}", session);
                }
                // 将加载的密钥对缓存到Cache和Redis
                cacheService.put(getServerConfig(), session, loadedKey);
                return loadedKey;
            }
        }
        return null;
    }

    public RsaKey create(String session) {
        RsaKey key = load(session);
        // 如果数据库也没有，则生成新的
        return null != key ? key : generate(session);
    }

    /**
     * 获取密钥对（如果不存在则生成新的）
     * 返回的密钥对包含过期时间，客户端应在此时间之前重新获取
     * 仅在客户端主动请求获取密钥时调用，如果密钥即将过期则生成新的
     *
     * @param session 客户端UUID标识
     * @return 包含过期时间的密钥对
     * @throws CodeException 密钥获取或生成失败时抛出异常
     */
    public RsaKey getKey(String session) throws CodeException {
        try {
            if (StringUtils.isBlank(session)) {
                throw new CodeException(Error.RSA_SESSION_REQUIRED);
            }
            EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
            // 使用compute方法：如果缓存不存在则生成，存在则返回
            RsaKey pair = cacheService.compute(session, () -> create(session), getServerConfig());
            // 检查密钥对是否有效：为null、已过期、格式无效或即将过期时，删除缓存并重新调用compute
            // 利用短路求值：如果pair为null，后面的条件不会执行
            if (pair == null || pair.expired() || StringUtils.isBlank(pair.getPublicKey()) || StringUtils.isBlank(pair.getPrivateKey()) || pair.expiring(config.getClientBufferMinutes())) {
                if (pair != null && log.isDebugEnabled())
                    log.debug("删除重建:{}, KEY:{}, 向量:{}, 过期:{}, 临期:{}, 过期时间:{}", pair.getSession(), pair.getPublicKey(), pair.getPrivateKey(), pair.expired(), pair.expiring(), null != pair.getExpireTime() ? new Date(pair.getExpireTime()) : "");
                // 删除缓存并重新调用compute
                cacheService.remove(getServerConfig().getName(), session);
                pair = cacheService.compute(session, () -> create(session), getServerConfig());
            }
            if (log.isDebugEnabled())
                log.debug("获取密钥:{}, KEY:{}, 向量:{}, 过期:{}, 临期:{}, 过期时间:{}", pair.getSession(), pair.getPublicKey(), pair.getPrivateKey(), pair.expired(), pair.expiring(), null != pair.getExpireTime() ? new Date(pair.getExpireTime()) : "");
            return pair;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CodeException) {
                throw (CodeException) e.getCause();
            }
            if (log.isDebugEnabled())
                log.debug("获取密钥对:{}, 错误:{}", session, e.getMessage());
            throw new CodeException(Error.RSA_KEY_PAIR_NOT_FOUND);
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("获取密钥对:{}, 异常:{}", session, e.getMessage());
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
            throw new CodeException(Error.RSA_SESSION_REQUIRED);
        }
        // 从缓存中获取密钥对
        RsaKey rsaKey = cacheService.get(getServerConfig(), value.getSession());
        if (rsaKey == null) {
            rsaKey = load(value.getSession());
            if (null == rsaKey)
                throw new CodeException(Error.RSA_PRIVATE_KEY_NOT_FOUND);
        }
        String privateKey = rsaKey.getPrivateKey();
        if (StringUtils.isBlank(privateKey)) {
            throw new CodeException(Error.RSA_PRIVATE_KEY_NOT_FOUND);
        }
        // 检查密钥对是否已过期（但仍在服务端冗余保留时间内）
        if (rsaKey.expired()) {
            log.warn("使用已过期的密钥对进行解密，Session: {}, 过期时间: {}", value.getSession(), rsaKey.getExpireTime());
        }
        // 执行解密
        try {
            String decrypted = RsaUtil.decrypt(value.getCiphertext(), privateKey);
            if (StringUtils.isBlank(decrypted)) {
                log.warn("解密结果为空，Session: {}", value.getSession());
            }
            return decrypted;
        } catch (IllegalArgumentException e) {
            log.error("RSA密钥格式错误，解密失败，Session: {}, 错误: {}", value.getSession(), e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("RSA解密失败，Session: {}, 错误: {}", value.getSession(), e.getMessage(), e);
            throw new CodeException(Error.RSA_DECRYPT_FAILED);
        }
    }

    /**
     * 保存客户端公钥（使用UUID作为客户端标识）
     *
     * @param session    客户端UUID标识
     * @param publicKey  客户端公钥
     * @param expireTime 客户端指定的过期时间（毫秒时间戳），如果为null则使用后端默认过期时间
     * @return 保存的客户端公钥信息
     * @throws CodeException 保存失败时抛出异常
     */
    public RsaKey savePublicKey(String session, String publicKey, Long expireTime) throws CodeException {
        if (StringUtils.isBlank(session)) {
            throw new CodeException(Error.RSA_SESSION_REQUIRED);
        }
        if (StringUtils.isBlank(publicKey)) {
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        }
        // 验证公钥格式：尝试使用公钥加密一个测试字符串来验证格式
        try {
            String testData = "test";
            RsaUtil.encrypt(testData, publicKey);
        } catch (IllegalArgumentException e) {
            log.error("客户端公钥格式错误，Session: {}, 错误: {}", session, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (RuntimeException e) {
            // RsaUtil.encrypt可能抛出RuntimeException包装的InvalidKeyException
            Throwable cause = e.getCause();
            if (cause instanceof java.security.InvalidKeyException) {
                log.error("客户端公钥解析失败，Session: {}, 错误: {}", session, cause.getMessage());
                throw new CodeException(Error.RSA_PUBLIC_KEY_PARSE_ERROR);
            }
            log.error("客户端公钥验证失败，Session: {}, 错误: {}", session, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("客户端公钥验证失败，Session: {}, 错误: {}", session, e.getMessage());
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
                if (log.isDebugEnabled())
                    log.warn("客户端指定的过期时间超过最大有效期，使用最大有效期。Session: {}, 客户端指定: {}, 最大有效期: {}", session, expireTime, maxExpireTime);
                finalExpireTime = maxExpireTime;
            } else {
                finalExpireTime = expireTime;
                if (log.isDebugEnabled()) {
                    log.debug("使用客户端指定的过期时间，Session: {}, 过期时间: {}", session, finalExpireTime);
                }
            }
        } else {
            // 客户端未提供过期时间，使用后端默认过期时间
            finalExpireTime = maxExpireTime;
            if (log.isDebugEnabled()) {
                log.debug("使用后端默认过期时间，Session: {}, 过期时间: {}", session, finalExpireTime);
            }
        }
        // 创建客户端公钥对象
        RsaKey clientPublicKey = new RsaKey(session, publicKey, finalExpireTime);
        // 保存到缓存
        cacheService.put(getClientConfig(), session, clientPublicKey);
        if (log.isDebugEnabled()) {
            log.debug("保存客户端公钥，Session: {}, 过期时间: {}", session, finalExpireTime);
        }
        return clientPublicKey;
    }

    /**
     * 获取客户端公钥
     *
     * @param session 客户端标识
     * @return 客户端公钥信息，如果不存在或已过期返回null
     */
    public RsaKey getClientPublicKey(String session) {
        if (StringUtils.isBlank(session)) {
            return null;
        }
        RsaKey rsaKey = cacheService.get(getClientConfig(), session);
        if (rsaKey == null) {
            // 如果缓存中没有，尝试从数据库加载
            if (encryptionLoader != null) {
                String publicKeyStr = encryptionLoader.loadClient(session);
                if (StringUtils.isNotBlank(publicKeyStr)) {
                    // 构建RsaKey对象
                    rsaKey = new RsaKey();
                    rsaKey.setSession(session);
                    rsaKey.setPublicKey(publicKeyStr);
                    // 从数据库加载时，过期时间需要从实体中获取，这里暂时不设置
                    // 将加载的公钥缓存到Cache和Redis
                    cacheService.put(getClientConfig(), session, rsaKey);
                    if (log.isDebugEnabled()) {
                        log.debug("从数据库加载客户端公钥成功，session: {}", session);
                    }
                }
            }
        }
        if (rsaKey != null && rsaKey.expired()) {
            if (log.isDebugEnabled())
                log.debug("客户端公钥已过期，Session: {}, 过期时间: {}", session, rsaKey.getExpireTime());
            // 可以选择删除过期公钥或返回null
            return null;
        }
        return rsaKey;
    }

    /**
     * 使用客户端公钥加密数据（使用UUID）
     * 服务端使用客户端的公钥加密数据，返回给客户端，客户端使用自己的私钥解密
     *
     * @param data    待加密的数据
     * @param session 客户端UUID标识
     * @return Base64编码的加密数据
     * @throws CodeException 加密失败时抛出异常
     */
    public String encrypt(String data, String session) throws CodeException {
        if (StringUtils.isBlank(data)) {
            return "";
        }
        if (StringUtils.isBlank(session)) {
            throw new IllegalArgumentException("Session不能为空");
        }
        // 获取客户端公钥
        RsaKey clientPublicKey = getClientPublicKey(session);
        if (clientPublicKey == null) {
            throw new CodeException(Error.RSA_CLIENT_PUBLIC_KEY_NOT_FOUND);
        }
        // 检查公钥是否即将过期
        EncryptConfigHandler.RsaConfig config = encryptConfigFactory.getRsaConfig();
        if (clientPublicKey.expiring(config.getClientBufferMinutes())) {
            if (log.isDebugEnabled())
                log.warn("客户端公钥即将过期，建议客户端更新公钥，Session: {}, 过期时间: {}", session, clientPublicKey.getExpireTime());
        }
        try {
            // 使用客户端公钥加密
            String encrypted = RsaUtil.encrypt(data, clientPublicKey.getPublicKey());
            if (StringUtils.isBlank(encrypted)) {
                log.warn("加密结果为空，Session: {}", session);
                throw new CodeException(Error.RSA_ENCRYPT_FAILED);
            }
            return encrypted;
        } catch (IllegalArgumentException e) {
            log.error("RSA密钥格式错误，加密失败，Session: {}, 错误: {}", session, e.getMessage());
            throw new CodeException(Error.RSA_KEY_FORMAT_ERROR);
        } catch (Exception e) {
            log.error("使用客户端公钥加密失败，Session: {}, 错误: {}", session, e.getMessage(), e);
            throw new CodeException(Error.RSA_ENCRYPT_FAILED);
        }
    }

    /**
     * 检查客户端公钥是否存在且有效
     *
     * @param session 客户端标识
     * @return true表示存在且有效，false表示不存在或已过期
     */
    public boolean hasValidClientPublicKey(String session) {
        RsaKey clientPublicKey = getClientPublicKey(session);
        return clientPublicKey != null && !clientPublicKey.expired();
    }

    @Override
    public void must() {
        try {
            List<EndpointInfo> list = getEncryptEndpoints();
            log.info("Scan encrypt endpoints:{}", list.size());
        } catch (CodeException e) {
            log.error("加密端点:{}", e.getMessage());
        }
    }

    public List<EndpointInfo> getEncryptEndpoints() throws CodeException {
        if (null != result)
            return new ArrayList<>(result);
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
                if (StringUtils.isNotBlank(basePath) && !basePath.startsWith("/"))
                    basePath = "/" + basePath;
                if (StringUtils.isNotBlank(methodPath) && !basePath.endsWith("/") && !methodPath.startsWith("/"))
                    methodPath = "/" + methodPath;
                // 构建完整路径
                String fullPath = basePath + methodPath;
                // 检查请求body参数类型
                Parameter[] parameters = method.getParameters();
                boolean hasEncryptBody = false;
                boolean forceEncryptBody = false;
                for (Parameter parameter : parameters) {
                    RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
                    if (requestBody != null) {
                        Class<?> paramType = parameter.getType();
                        if (isEncryptType(paramType)) {
                            hasEncryptBody = true;
                            // 检查参数上的@Endpoint注解，如果forceEncrypt=true，则设置force.body=true
                            Endpoint paramEndpoint = parameter.getAnnotation(Endpoint.class);
                            if (paramEndpoint != null && paramEndpoint.force()) {
                                forceEncryptBody = true;
                            }
                        }
                    }
                }
                // 检查返回类型
                Class<?> returnType = method.getReturnType();
                boolean isEncryptReturnType = isEncryptType(returnType);
                boolean forceEncryptReturn = methodEndpoint != null && methodEndpoint.force() && isEncryptReturnType;
                // 检查方法上的@Endpoint注解，如果force=true，则设置force.ret=true
                // 如果请求body或返回值类型是Encrypt，则添加到结果中
                if (hasEncryptBody || isEncryptReturnType) {
                    EndpointInfo endpointInfo = new EndpointInfo();
                    endpointInfo.setPath(fullPath);
                    endpointInfo.setMethod(httpMethod);
                    // 设置param支持加密信息
                    Supported param = new Supported();
                    param.setBody(hasEncryptBody);
                    param.setRet(isEncryptReturnType);
                    endpointInfo.setParam(param);
                    // 设置force强制加密信息
                    Supported force = new Supported();
                    force.setBody(forceEncryptBody);
                    force.setRet(forceEncryptReturn);
                    endpointInfo.setForce(force);
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
