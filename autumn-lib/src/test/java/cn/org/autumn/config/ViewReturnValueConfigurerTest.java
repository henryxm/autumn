package cn.org.autumn.config;

import static org.junit.Assert.assertTrue;

import cn.org.autumn.handler.ViewNameReturnValueHandler;
import cn.org.autumn.view.ViewTemplateSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;

public class ViewReturnValueConfigurerTest {

    @Test
    public void wrapReplacesViewNameHandlerOnce() {
        RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
        handlers.add(new ViewNameMethodReturnValueHandler());
        adapter.setReturnValueHandlers(handlers);

        ViewReturnValueConfigurer configurer = new ViewReturnValueConfigurer();
        ReflectionTestUtils.setField(configurer, "requestMappingHandlerAdapter", adapter);
        ReflectionTestUtils.setField(configurer, "viewTemplateSupport", new ViewTemplateSupport());
        configurer.wrapViewNameReturnValueHandler();

        assertTrue(adapter.getReturnValueHandlers().get(0) instanceof ViewNameReturnValueHandler);
        configurer.wrapViewNameReturnValueHandler();
        assertTrue(adapter.getReturnValueHandlers().get(0) instanceof ViewNameReturnValueHandler);
    }
}
