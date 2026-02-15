package cn.org.autumn.config;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Swagger 配置处理器接口。
 * <p>已迁移至 springdoc-openapi，API 文档通过 {@code SwaggerConfig} 中的 OpenAPI Bean 配置。</p>
 */
@Component
@ConditionalOnMissingBean(SwaggerHandler.class)
@Order(Integer.MAX_VALUE / 1000)
public interface SwaggerHandler {
}
