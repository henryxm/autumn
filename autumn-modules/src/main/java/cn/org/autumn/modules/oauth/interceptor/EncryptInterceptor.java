package cn.org.autumn.modules.oauth.interceptor;

import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.service.AesService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * AES加密解密拦截器
 * 处理请求解密和响应加密
 * <p>
 * 使用AES对称加密算法进行数据传输加密，相比RSA具有更高的性能
 *
 * @author Autumn
 */
@Slf4j
@Component
@ControllerAdvice
public class EncryptInterceptor implements HandlerInterceptor, InterceptorHandler, ResponseBodyAdvice<Object> {

    @Autowired
    private AesService aesService;

    /**
     * 请求属性键：标记请求是否被加密
     */
    private static final String REQUEST_ENCRYPTED_ATTR = "REQUEST_ENCRYPTED";
    /**
     * 请求属性键：存储请求的UUID
     */
    private static final String REQUEST_UUID_ATTR = "REQUEST_UUID";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 只处理HandlerMethod类型的handler
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Parameter[] parameters = handlerMethod.getMethod().getParameters();
        // 检查方法参数中是否有Encrypt类型的参数
        boolean hasEncryptParameter = false;
        for (Parameter parameter : parameters) {
            // 检查是否有@RequestBody注解
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                Class<?> parameterType = parameter.getType();
                // 检查是否是Encrypt类型
                if (Encrypt.class.isAssignableFrom(parameterType)) {
                    hasEncryptParameter = true;
                    break;
                }
            }
        }
        if (hasEncryptParameter) {
            String requestBody = null;
            if (request instanceof CachedBodyHttpServletRequest) {
                requestBody = ((CachedBodyHttpServletRequest) request).getBody();
            } else {
                // 后备方案：如果Filter没有包装请求，尝试直接读取请求体
                // 注意：这只能读取一次，如果请求体已经被读取过，这里会失败
                try {
                    BufferedReader reader = request.getReader();
                    if (reader != null) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        requestBody = sb.toString();
                    }
                } catch (Exception e) {
                    try {
                        requestBody = org.springframework.util.StreamUtils.copyToString(
                                request.getInputStream(), StandardCharsets.UTF_8);
                    } catch (IOException e2) {
                        // 无法读取请求体，可能是非JSON请求或已被读取，继续正常流程
                        return true;
                    }
                }
            }
            // 缓存请求体，供参数解析器使用
            if (StringUtils.isNotBlank(requestBody)) {
                request.setAttribute("CACHED_REQUEST_BODY", requestBody);
                // 尝试解析为Encrypt对象，检查是否是加密请求
                for (Parameter parameter : parameters) {
                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        Class<?> parameterType = parameter.getType();
                        if (Encrypt.class.isAssignableFrom(parameterType)) {
                            try {
                                Encrypt encryptObj = (Encrypt) JSON.parseObject(requestBody, parameterType);
                                if (encryptObj != null && StringUtils.isNotBlank(encryptObj.getEncrypt())
                                        && StringUtils.isNotBlank(encryptObj.getUuid())) {
                                    // 标记为加密请求
                                    request.setAttribute(REQUEST_ENCRYPTED_ATTR, true);
                                    request.setAttribute(REQUEST_UUID_ATTR, encryptObj.getUuid());
                                    request.setAttribute("ENCRYPT_OBJ", encryptObj);
                                }
                            } catch (Exception e) {
                                // 解析失败，可能不是加密请求，继续正常处理
                            }
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }

    @Override
    public List<String> getPatterns() {
        return Collections.singletonList("/**");
    }

    @Override
    public List<String> getExcludePatterns() {
        return null;
    }

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        // 检查请求是否被加密
        HttpServletRequest httpRequest = getHttpServletRequest(request);
        if (httpRequest == null) {
            return body;
        }
        // 排除RSA相关的接口（密钥交换接口），避免循环加密
        String requestURI = httpRequest.getRequestURI();
        if (requestURI != null && requestURI.startsWith("/rsa/")) {
            return body;
        }
        Boolean isEncrypted = (Boolean) httpRequest.getAttribute(REQUEST_ENCRYPTED_ATTR);
        String uuid = (String) httpRequest.getAttribute(REQUEST_UUID_ATTR);

        // 只有明确标记为加密的请求才加密响应
        if (isEncrypted == null || !isEncrypted || StringUtils.isBlank(uuid)) {
            return body;
        }
        // 只对实现了Encrypt接口的响应进行加密
        if (body instanceof Encrypt) {
            try {
                // 将响应体序列化为JSON
                String jsonBody = JSON.toJSONString(body);
                // 使用AES密钥加密响应数据
                String encryptedData = aesService.encrypt(jsonBody, uuid);
                // 使用反射创建返回值类型的实例
                return createEncryptedResponse(body.getClass(), encryptedData, uuid);
            } catch (CodeException e) {
                log.error("响应加密失败，UUID: {}, 错误: {}", uuid, e.getMessage());
                return body;
            } catch (Exception e) {
                log.error("响应加密处理失败，UUID: {}", uuid, e);
                return body;
            }
        }
        return body;
    }

    /**
     * 使用反射创建加密响应对象
     *
     * @param responseClass 响应类型
     * @param data          加密后的数据
     * @param uuid          UUID
     * @return 加密响应对象
     */
    private Object createEncryptedResponse(Class<?> responseClass, String data, String uuid) throws Exception {
        // 使用反射创建实例
        Object instance = responseClass.getDeclaredConstructor().newInstance();
        // 设置 encrypt 字段
        setFieldValue(instance, "encrypt", data);
        // 设置 uuid 字段
        setFieldValue(instance, "uuid", uuid);
        // 如果是 Response 类型，设置默认值
        if (cn.org.autumn.model.Response.class.isAssignableFrom(responseClass)) {
            setFieldValue(instance, "code", 0);
            setFieldValue(instance, "msg", null);
            setFieldValue(instance, "data", null);
        }
        return instance;
    }

    /**
     * 使用反射设置字段值
     * 优先使用 setter 方法，如果没有则直接设置字段
     *
     * @param obj       对象实例
     * @param fieldName 字段名
     * @param value     字段值
     */
    private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Class<?> clazz = obj.getClass();
        // 尝试使用 setter 方法
        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                // 检查参数类型是否匹配（包括基本类型和包装类型）
                if (isAssignable(paramType, value)) {
                    method.invoke(obj, value);
                    return;
                }
            }
        }
        // 如果没有找到 setter，尝试直接设置字段
        try {
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            // 字段不存在，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null) {
                try {
                    java.lang.reflect.Field field = superClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(obj, value);
                    return;
                } catch (NoSuchFieldException ex) {
                    superClass = superClass.getSuperclass();
                }
            }
            // 如果所有父类都没有找到字段，记录警告但不抛出异常
            log.warn("无法设置字段 {} 的值，字段不存在或无法访问", fieldName);
        }
    }

    /**
     * 检查值是否可以赋值给指定类型
     * 处理基本类型和包装类型的转换
     */
    private boolean isAssignable(Class<?> paramType, Object value) {
        if (value == null) {
            return !paramType.isPrimitive();
        }
        // 直接类型匹配
        if (paramType.isAssignableFrom(value.getClass())) {
            return true;
        }
        // 处理基本类型和包装类型的转换
        if (paramType.isPrimitive()) {
            if (paramType == int.class && value instanceof Integer) {
                return true;
            }
            if (paramType == long.class && value instanceof Long) {
                return true;
            }
            if (paramType == double.class && value instanceof Double) {
                return true;
            }
            if (paramType == float.class && value instanceof Float) {
                return true;
            }
            if (paramType == boolean.class && value instanceof Boolean) {
                return true;
            }
            if (paramType == byte.class && value instanceof Byte) {
                return true;
            }
            if (paramType == short.class && value instanceof Short) {
                return true;
            }
            if (paramType == char.class && value instanceof Character) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从ServerHttpRequest获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            return ((ServletServerHttpRequest) request).getServletRequest();
        }
        return null;
    }
}