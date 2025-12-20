package cn.org.autumn.modules.oauth.resolver;

import cn.org.autumn.config.ResolverHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.modules.oauth.interceptor.CachedBodyHttpServletRequest;
import cn.org.autumn.service.AesService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Encrypt参数解析器
 * 处理加密请求参数的解密
 * <p>
 * 使用AES对称加密算法进行请求数据解密，相比RSA具有更高的性能
 *
 * @author Autumn
 */
@Slf4j
@Component
public class EncryptArgumentResolver implements HandlerMethodArgumentResolver, ResolverHandler {

    @Autowired
    private AesService aesService;

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return Encrypt.class.isAssignableFrom(parameter.getParameterType()) && parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("无法获取HttpServletRequest");
        }
        // 检查 @RequestBody 的 required 属性
        RequestBody requestBodyAnnotation = parameter.getParameterAnnotation(RequestBody.class);
        boolean required = requestBodyAnnotation == null || requestBodyAnnotation.required();
        CachedBodyHttpServletRequest cachedRequest = request instanceof CachedBodyHttpServletRequest ? (CachedBodyHttpServletRequest) request : null;
        Encrypt encrypt = (Encrypt) request.getAttribute("ENCRYPT_OBJ");
        // 如果检测到加密对象，必须成功解密，否则抛出异常
        if (encrypt != null && StringUtils.isNotBlank(encrypt.getEncrypt())) {
            // 获取UUID，优先使用encrypt对象中的UUID，如果没有则从请求属性或Session中获取
            String uuid = encrypt.getUuid();
            if (StringUtils.isBlank(uuid)) {
                // 尝试从请求属性中获取（由EncryptInterceptor设置）
                uuid = (String) request.getAttribute("REQUEST_UUID_ATTR");
            }
            if (StringUtils.isBlank(uuid)) {
                // 如果还是没有，尝试从请求属性或Header中获取
                uuid = getUuid(request);
            }
            if (StringUtils.isNotBlank(uuid) && StringUtils.isNotBlank(encrypt.getEncrypt())) {
                try {
                    // 使用AES密钥解密请求数据
                    String decryptedJson = aesService.decrypt(encrypt.getEncrypt(), uuid);
                    // 获取包含泛型信息的Type，以正确处理泛型类型（如Page<Window>）
                    Type parameterType = getParameterType(parameter);
                    return JSON.parseObject(decryptedJson, parameterType);
                } catch (CodeException e) {
                    log.error("解密失败，UUID: {}, 错误: {}", uuid, e.getMessage());
                    throw e;
                }
            }
        }
        // 如果没有加密标记，尝试正常解析请求体
        String requestBody = getRequestBody(request, cachedRequest);
        // 如果请求体为空
        if (StringUtils.isBlank(requestBody)) {
            // 如果 required = false，允许返回 null
            if (!required) {
                if (log.isDebugEnabled()) {
                    log.debug("请求体为空，但参数为非必填，返回null。请求方式:{}, 路径:{}", request.getMethod(), request.getRequestURI());
                }
                return null;
            }
            // 如果 required = true，检查是否是GET等无body的请求方法
            String method = request.getMethod();
            if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
                if (log.isDebugEnabled()) {
                    log.debug("{}请求通常没有请求体，返回null。路径:{}", method, request.getRequestURI());
                }
                return null;
            }
            // 其他情况，如果required=true，抛出异常
            throw new IllegalStateException("请求体为空");
        }
        // 尝试解析请求体
        try {
            // 获取包含泛型信息的Type，以正确处理泛型类型（如Page<Client>）
            Type parameterType = getParameterType(parameter);
            return JSON.parseObject(requestBody, parameterType);
        } catch (Exception e) {
            log.error("参数解析失败:{}", e.getMessage());
            throw new IllegalStateException("参数解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取参数类型，支持泛型类型
     * 对于泛型类型（如Page<Client>），返回包含泛型信息的Type
     * 对于普通类型，返回Class
     *
     * @param parameter 方法参数
     * @return 参数类型（Type或Class）
     */
    private Type getParameterType(MethodParameter parameter) {
        // 使用ResolvableType来获取包含泛型信息的Type
        ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
        Type type = resolvableType.getType();
        // 如果Type是Class类型（非泛型），直接返回
        if (type instanceof Class) {
            return type;
        }
        // 如果是ParameterizedType（泛型类型），返回完整的Type信息
        // 这样Fastjson就能正确解析泛型字段（如Page<Client>中的data字段）
        return type;
    }

    /**
     * 获取请求体内容
     * 优先从缓存中获取，如果无法获取则返回null，不抛出异常
     *
     * @param request       HTTP请求
     * @param cachedRequest 缓存的请求包装器
     * @return 请求体内容，如果无法获取则返回null
     */
    private String getRequestBody(HttpServletRequest request, CachedBodyHttpServletRequest cachedRequest) {
        // 1. 优先从CachedBodyHttpServletRequest获取
        if (cachedRequest != null) {
            try {
                String body = cachedRequest.getBody();
                if (StringUtils.isNotBlank(body)) {
                    return body;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("从CachedBodyHttpServletRequest读取请求体失败: {}", e.getMessage());
                }
            }
        }
        // 2. 从拦截器缓存的请求体获取
        String cachedBody = (String) request.getAttribute("CACHED_REQUEST_BODY");
        if (cachedBody != null) {
            return cachedBody;
        }
        // 3. 兼容 MultiRequestBodyArgumentResolver 的缓存
        cachedBody = (String) request.getAttribute("JSON_REQUEST_BODY");
        if (cachedBody != null) {
            return cachedBody;
        }
        // 4. 尝试从 InputStream 读取（可能已经被读取过，会失败）
        // 注意：这里不抛出异常，而是返回null，让调用方决定如何处理
        try {
            // 检查Content-Length，如果为0或-1，说明没有请求体
            long contentLength = request.getContentLengthLong();
            if (contentLength <= 0) {
                if (log.isDebugEnabled()) {
                    log.debug("请求Content-Length为{}，没有请求体", contentLength);
                }
                return null;
            }
            String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(body)) {
                // 缓存读取到的请求体，供后续使用
                request.setAttribute("JSON_REQUEST_BODY", body);
                return body;
            }
            return null;
        } catch (Exception e) {
            // 请求体可能已被其他组件读取，或者请求本身就没有body（如GET请求）
            // 不抛出异常，返回null，让调用方根据required属性决定如何处理
            if (log.isDebugEnabled()) {
                log.debug("无法读取请求体，可能已被其他组件读取或请求本身没有body。请求方式:{}, 路径:{}, 错误:{}", request.getMethod(), request.getRequestURI(), e.getMessage());
            }
            return null;
        }
    }

    /**
     * 获取UUID
     * 优先级：请求属性（由EncryptInterceptor设置） > X-Encrypt-UUID header
     * 只有在明确指定了X-Encrypt-UUID header时才返回UUID，避免误判
     *
     * @param request HTTP请求
     * @return UUID，如果未找到则返回null
     */
    private String getUuid(HttpServletRequest request) {
        // 1. 优先从请求属性中获取（由EncryptInterceptor设置）
        String uuid = (String) request.getAttribute("REQUEST_UUID_ATTR");
        if (StringUtils.isNotBlank(uuid)) {
            return uuid;
        }
        
        // 2. 从X-Encrypt-UUID header中获取（只有在明确指定时才返回）
        uuid = request.getHeader("X-Encrypt-UUID");
        if (StringUtils.isNotBlank(uuid)) {
            return uuid.trim();
        }
        
        return null;
    }

    @Override
    public HandlerMethodArgumentResolver getResolver() {
        return this;
    }
}
