package cn.org.autumn.modules.oauth.resolver;

import cn.org.autumn.config.ResolverHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Encrypt;
import cn.org.autumn.modules.oauth.interceptor.CachedBodyHttpServletRequest;
import cn.org.autumn.service.RsaService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Encrypt参数解析器
 * 处理加密请求参数的解密
 *
 * @author Autumn
 */
@Slf4j
@Component
public class EncryptArgumentResolver implements HandlerMethodArgumentResolver, ResolverHandler {

    @Autowired
    private RsaService rsaService;

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
        CachedBodyHttpServletRequest cachedRequest = request instanceof CachedBodyHttpServletRequest ? (CachedBodyHttpServletRequest) request : null;
        Encrypt encryptObj = (Encrypt) request.getAttribute("ENCRYPT_OBJ");
        if (encryptObj != null && StringUtils.isNotBlank(encryptObj.getEncrypt())
                && StringUtils.isNotBlank(encryptObj.getUuid())) {
            // 如果检测到加密对象，必须成功解密，否则抛出异常
            try {
                String decryptedJson = rsaService.decrypt(encryptObj);
                return JSON.parseObject(decryptedJson, parameter.getParameterType());
            } catch (CodeException e) {
                log.error("参数解密失败，UUID: {}, 错误: {}", encryptObj.getUuid(), e.getMessage());
                throw e;
            }
        }
        
        // 如果没有加密标记，尝试正常解析请求体
        String requestBody = getRequestBody(request, cachedRequest);
        if (StringUtils.isBlank(requestBody)) {
            throw new IllegalStateException("请求体为空");
        }
        try {
            return JSON.parseObject(requestBody, parameter.getParameterType());
        } catch (Exception e) {
            log.error("参数解析失败", e);
            throw new IllegalStateException("参数解析失败: " + e.getMessage(), e);
        }
    }

    private String getRequestBody(HttpServletRequest request, CachedBodyHttpServletRequest cachedRequest) throws IOException {
        if (cachedRequest != null) {
            return cachedRequest.getBody();
        }
        
        // 优先使用拦截器缓存的请求体
        String cachedBody = (String) request.getAttribute("CACHED_REQUEST_BODY");
        if (cachedBody != null) {
            return cachedBody;
        }
        
        // 兼容 MultiRequestBodyArgumentResolver 的缓存
        cachedBody = (String) request.getAttribute("JSON_REQUEST_BODY");
        if (cachedBody != null) {
            return cachedBody;
        }
        
        // 如果都没有，尝试从 InputStream 读取（可能已经被读取过，会失败）
        try {
            String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            request.setAttribute("JSON_REQUEST_BODY", body);
            return body;
        } catch (Exception e) {
            throw new IllegalStateException("无法读取请求体，可能已被其他组件读取", e);
        }
    }

    @Override
    public HandlerMethodArgumentResolver getResolver() {
        return this;
    }
}
