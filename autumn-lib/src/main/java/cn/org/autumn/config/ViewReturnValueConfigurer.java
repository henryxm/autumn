package cn.org.autumn.config;

import cn.org.autumn.annotation.AllowPostConstructDuringInstall;
import cn.org.autumn.handler.ViewNameReturnValueHandler;
import cn.org.autumn.view.ViewTemplateSupport;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;

/**
 * 将 {@link ViewNameReturnValueHandler} 注册进 MVC 返回值处理链。
 * <p>
 * 必须在 {@link RequestMappingHandlerAdapter} 完成默认 Handler 装配后执行（{@code @PostConstruct}），
 * 因此独立为本组件，而不写入 {@link WebConfig}，避免与参数解析器初始化逻辑耦合。
 * <p>
 * {@link ViewTemplateSupport} 使用 {@code @Lazy}：Configurer 本身在启动早期即可创建，
 * 不会反向拉动 {@link cn.org.autumn.site.TemplateFactory} 过早实例化 {@link freemarker.cache.TemplateLoader}。
 */
@Slf4j
@Component
@AllowPostConstructDuringInstall
public class ViewReturnValueConfigurer {

    @Autowired(required = false)
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Autowired(required = false)
    @Lazy
    private ViewTemplateSupport viewTemplateSupport;

    @PostConstruct
    public void wrapViewNameReturnValueHandler() {
        if (requestMappingHandlerAdapter == null || viewTemplateSupport == null) {
            return;
        }
        try {
            List<HandlerMethodReturnValueHandler> handlers = requestMappingHandlerAdapter.getReturnValueHandlers();
            if (handlers == null || handlers.isEmpty()) {
                return;
            }
            // 防止容器重复刷新时多次包装
            if (handlers.stream().anyMatch(h -> h instanceof ViewNameReturnValueHandler)) {
                return;
            }
            List<HandlerMethodReturnValueHandler> wrapped = new ArrayList<>(handlers);
            for (int i = 0; i < wrapped.size(); i++) {
                HandlerMethodReturnValueHandler handler = wrapped.get(i);
                if (handler instanceof ViewNameMethodReturnValueHandler) {
                    wrapped.set(i, new ViewNameReturnValueHandler((ViewNameMethodReturnValueHandler) handler, viewTemplateSupport));
                    requestMappingHandlerAdapter.setReturnValueHandlers(wrapped);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Failed to wrap view return value handler:{}", e.getMessage());
        }
    }
}
