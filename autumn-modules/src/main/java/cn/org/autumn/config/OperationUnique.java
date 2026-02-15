package cn.org.autumn.config;

import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

/**
 * 自定义 operationId 去重：如果 @Operation 注解指定了 operationId，则使用该值。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1000)
public class OperationUnique implements OperationCustomizer {

    @Override
    public io.swagger.v3.oas.models.Operation customize(io.swagger.v3.oas.models.Operation operation, HandlerMethod handlerMethod) {
        Operation annotation = handlerMethod.getMethodAnnotation(Operation.class);
        if (annotation != null && StringUtils.hasText(annotation.operationId())) {
            operation.setOperationId(annotation.operationId());
        }
        return operation;
    }
}
