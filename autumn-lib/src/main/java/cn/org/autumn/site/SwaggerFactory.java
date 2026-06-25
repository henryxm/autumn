package cn.org.autumn.site;

import cn.org.autumn.config.SwaggerHandler;
import java.util.List;
import org.springframework.stereotype.Component;

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
