package cn.org.autumn.modules.oauth.resolver;

import cn.org.autumn.model.Encrypt;
import cn.org.autumn.service.AesService;
import cn.org.autumn.service.RsaService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.lang.reflect.Type;
import java.util.List;

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
public class EncryptArgumentResolver extends RequestResponseBodyMethodProcessor {

    @Autowired
    private AesService aesService;

    @Autowired
    private RsaService rsaService;

    @Autowired
    Gson gson;

    /**
     * 构造函数：注入HttpMessageConverter列表
     * Spring Boot会自动配置HttpMessageConverter列表（包括Jackson等）
     */
    public EncryptArgumentResolver(List<HttpMessageConverter<?>> converters) {
        super(converters);
    }

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return Encrypt.class.isAssignableFrom(parameter.getParameterType()) && parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        Object object = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        if (object instanceof Encrypt) {
            Encrypt encrypt = (Encrypt) object;
            if (StringUtils.isNotBlank(encrypt.getCiphertext()) && StringUtils.isNotBlank(encrypt.getSession())) {
                long start = System.currentTimeMillis();
                //当使用RSA解密时，使用服务端的私钥进行解密
                String decrypt = "RSA".equals(encrypt.getAlgorithm()) ? rsaService.decrypt(encrypt) : aesService.decrypt(encrypt);
                Type parameterType = getParameterType(parameter);
                object = gson.fromJson(decrypt, parameterType);
                long end = System.currentTimeMillis();
                if (log.isDebugEnabled() && null != decrypt) {
                    log.debug("解密数据: 长度:{}, 耗时:{}毫秒", decrypt.length(), end - start);
                    log.debug("解密内容: {}", decrypt);
                }
            }
        }
        return object;
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
}
