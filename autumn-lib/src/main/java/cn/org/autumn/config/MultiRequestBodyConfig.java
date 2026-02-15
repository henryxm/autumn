package cn.org.autumn.config;

import cn.org.autumn.bean.MultiRequestBodyArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
public class MultiRequestBodyConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 添加MultiRequestBody参数解析器（添加到列表后面，不影响EncryptArgumentResolver的优先级）
        argumentResolvers.add(new MultiRequestBodyArgumentResolver());
    }

    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        // 解决中文乱码问题
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    /**
     * 使用 extendMessageConverters 追加转换器，而非 configureMessageConverters。
     * configureMessageConverters 一旦向列表添加元素，Spring MVC 会跳过默认转换器注册
     * （包括 Jackson 3 JSON 转换器），导致 @RestController 返回对象时
     * 触发 HttpMediaTypeNotAcceptableException。
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, responseBodyConverter());
    }
}