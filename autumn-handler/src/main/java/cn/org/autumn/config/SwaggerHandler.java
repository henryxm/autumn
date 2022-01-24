package cn.org.autumn.config;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Component
@ConditionalOnMissingBean(SwaggerHandler.class)
@Order(Integer.MAX_VALUE / 1000)
public interface SwaggerHandler {
    default Docket getDocket() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(getApiInfo())
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(Operation.class))
                .paths(PathSelectors.any())
                .build().forCodeGeneration(true);

    }

    default ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("Autumn")
                .description("Autumn Api 文档")
                .termsOfServiceUrl("http://www.autumn.org.cn")
                .version("1.0.0")
                .build();
    }
}
