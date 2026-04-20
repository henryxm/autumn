package cn.org.autumn.config;

import cn.org.autumn.jackson.FlexibleEnumJacksonModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 启用后，Jackson 反序列化枚举时除字符串/数字外，兼容形如 {@code {"name":"Like"}}、{@code {"value":"Comment"}} 等对象包装，
 * 并将 JSON 空字符串 {@code ""}、仅空白、无可解析字段的空对象等视为 {@code null}，避免默认枚举强制拒绝空串。
 * 便于 {@code Page}{@code <SomeEnum>}、{@code Request}{@code <SomeEnum>} 等与前端/移动端约定不一致时的互通。
 * <p>
 * 关闭：{@code autumn.jackson.enum-object-compatible=false}
 */
@Configuration
@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
@ConditionalOnProperty(prefix = "autumn.jackson", name = "enum-object-compatible", havingValue = "true", matchIfMissing = true)
public class FlexibleEnumJacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer autumnFlexibleEnumJacksonCustomizer() {
        return builder -> builder.modules(new FlexibleEnumJacksonModule());
    }
}
