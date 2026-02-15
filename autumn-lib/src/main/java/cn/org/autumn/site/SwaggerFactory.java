package cn.org.autumn.site;

import cn.org.autumn.config.SwaggerHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Swagger 工厂。
 * <p>已迁移至 springdoc-openapi，保留此类以兼容工厂体系。</p>
 */
@Component
public class SwaggerFactory extends Factory {
    List<SwaggerHandler> swaggerHandlers = null;

    public SwaggerHandler getSwaggerHandler() {
        if (null == swaggerHandlers) {
            swaggerHandlers = getOrderList(SwaggerHandler.class);
            for (SwaggerHandler swaggerHandler : swaggerHandlers) {
                return swaggerHandler;
            }
        }
        return new SwaggerHandler() {
        };
    }
}
