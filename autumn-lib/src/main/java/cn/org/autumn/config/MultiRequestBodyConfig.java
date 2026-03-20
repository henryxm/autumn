package cn.org.autumn.config;

import cn.org.autumn.bean.MultiRequestBodyArgumentResolver;
import cn.org.autumn.bean.QuotStringHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
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
        // 使用 QuotStringHttpMessageConverter：读取 @RequestBody String 时去掉首尾双引号/单引号，
        // 避免客户端发送 JSON 字符串（如 "hello"）时收到带引号的值，兼容升级后行为
        return new QuotStringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 插入到最前，使 @RequestBody String 优先使用本 converter，统一去掉首尾引号
        converters.add(0, responseBodyConverter());
    }
}