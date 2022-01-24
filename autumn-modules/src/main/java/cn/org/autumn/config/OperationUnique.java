package cn.org.autumn.config;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1000)
public class OperationUnique implements OperationBuilderPlugin {

    public void apply(OperationContext context) {
        Optional<Operation> methodAnnotation = context.findAnnotation(Operation.class);
        if (methodAnnotation.isPresent()) {
            Operation operation = methodAnnotation.get();
            if (StringUtils.hasText(operation.operationId())) {
                context.operationBuilder().uniqueId(operation.operationId());
                context.operationBuilder().codegenMethodNameStem(operation.operationId());
            }
        }
    }

    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}