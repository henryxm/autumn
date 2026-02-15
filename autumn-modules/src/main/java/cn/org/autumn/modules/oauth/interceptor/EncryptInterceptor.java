package cn.org.autumn.modules.oauth.interceptor;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.Response;
import cn.org.autumn.service.AesService;
import cn.org.autumn.service.RsaService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    @Autowired
    private RsaService rsaService;

    @Autowired
    Gson gson;

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
        Method method = returnType.getMethod();
        return method != null && Encrypt.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType, @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType, @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (null != body && log.isDebugEnabled()) {
            log.debug("原始返回:{}", gson.toJson(body));
        }
        // 检查请求是否被加密
        HttpServletRequest servlet = getHttpServletRequest(request);
        if (servlet == null || null == body) {
            return body;
        }
        // 排除RSA相关的接口（密钥交换接口），避免循环加密
        String uri = servlet.getRequestURI();
        if (uri != null && uri.startsWith("/rsa/")) {
            return body;
        }
        // 关键逻辑：只有当header中包含X-Encrypt-Session时，才进行响应加密
        // 如果没有这个header，直接返回body，使用之前的流程（完全兼容）
        String session = servlet.getHeader("X-Encrypt-Session");
        String algorithm = servlet.getHeader("X-Encrypt-Algorithm");
        if (StringUtils.isBlank(algorithm))
            algorithm = "AES";
        // 检查方法上的@Endpoint注解，如果force=true，则强制要求session
        Method controllerMethod = returnType.getMethod();
        if (controllerMethod != null) {
            Endpoint endpointAnnotation = controllerMethod.getAnnotation(Endpoint.class);
            if (endpointAnnotation != null && endpointAnnotation.force()) {
                // 强制加密验证：检查session是否为空
                if (StringUtils.isBlank(session)) {
                    log.error("强制加密接口缺少Session，方法: {}", controllerMethod.getName());
                    return Response.error(Error.FORCE_ENCRYPT_SESSION_REQUIRED);
                }
            }
        }
        if (StringUtils.isBlank(session)) {
            // 没有X-Encrypt-Session header，不进行响应加密，使用之前的流程
            return body;
        }
        // 只对实现了Encrypt接口的响应进行加密
        if (body instanceof Encrypt) {
            try {
                long start = System.currentTimeMillis();
                // 将响应体序列化为JSON
                String json = gson.toJson(body);
                //使用AES密钥加密响应数据 或者使用RSA进行加密
                //当使用RSA加密时，使用客户端的公钥进行加密
                String encrypt = "RSA".equals(algorithm) ? rsaService.encrypt(json, session) : aesService.encrypt(json, session);
                // 使用反射创建返回值类型的实例
                body = response(body, encrypt, algorithm, session);
                long end = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("加密长度:{}, 耗时:{}毫秒", json.length(), end - start);
                    log.debug("加密返回:{}, 内容:{}, 密文:{}", session, json, encrypt);
                }
            } catch (Exception e) {
                log.error("加密失败: {}, URI: {}, 错误: {}", session, uri, e.getMessage());
                return Response.error(e);
            }
        }
        return body;
    }

    /**
     * 使用反射创建加密响应对象
     *
     * @param body 响应类型
     * @param data 加密后的数据
     * @param uuid UUID
     * @return 加密响应对象
     */
    private Object response(Object body, String data, String algorithm, String uuid) throws Exception {
        Class<?> responseClass = body.getClass();
        // 使用反射创建实例
        Object instance = responseClass.getDeclaredConstructor().newInstance();
        // 设置 encrypt 字段
        setFieldValue(instance, "ciphertext", data);
        setFieldValue(instance, "algorithm", algorithm);
        // 设置 uuid 字段
        setFieldValue(instance, "session", uuid);
        // 如果是 Response 类型，设置默认值
        if (Response.class.isAssignableFrom(responseClass)) {
            Response<?> original = (Response<?>) body;
            setFieldValue(instance, "code", original.getCode());
            setFieldValue(instance, "msg", original.getMsg());
            setFieldValue(instance, "data", null);
            setFieldValue(instance, "result", null);
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
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            // 字段不存在，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null) {
                try {
                    Field field = superClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(obj, value);
                    return;
                } catch (NoSuchFieldException ex) {
                    superClass = superClass.getSuperclass();
                }
            }
            // 如果所有父类都没有找到字段，记录警告但不抛出异常
            if (log.isDebugEnabled())
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
            return paramType == char.class && value instanceof Character;
        }
        return false;
    }

    private HttpServletRequest getHttpServletRequest(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            return ((ServletServerHttpRequest) request).getServletRequest();
        }
        return null;
    }
}