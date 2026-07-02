package cn.org.autumn.handler;

import cn.org.autumn.view.ViewTemplateSupport;
import cn.org.autumn.view.ViewTemplateSupport.ResolvedView;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;

/**
 * 包装 Spring 默认 {@link ViewNameMethodReturnValueHandler}，在视图解析之前完成模板校验与路径规范化。
 * <p>
 * <b>解决的问题</b>：访问 SPM / {@code modules/**} 指向的不存在页面时，原先返回 HTTP 500
 *（{@code Circular view path}），后台 ERROR 日志；现统一为 HTTP 404 + {@code templates/404.html}。
 * <p>
 * <b>注册方式</b>：由 {@link cn.org.autumn.config.ViewReturnValueConfigurer} 在
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter} 初始化后替换原 Handler，
 * 业务代码无感知。
 */
public class ViewNameReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final ViewNameMethodReturnValueHandler delegate;

    private final ViewTemplateSupport viewTemplateSupport;

    public ViewNameReturnValueHandler(ViewNameMethodReturnValueHandler delegate, ViewTemplateSupport viewTemplateSupport) {
        this.delegate = delegate;
        this.viewTemplateSupport = viewTemplateSupport;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return delegate.supportsReturnType(returnType);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        Object resolved = returnValue;
        if (returnValue instanceof String) {
            resolved = resolveViewName((String) returnValue, webRequest);
        }
        delegate.handleReturnValue(resolved, returnType, mavContainer, webRequest);
    }

    private String resolveViewName(String viewName, NativeWebRequest webRequest) {
        ResolvedView resolved = viewTemplateSupport.resolve(viewName);
        if (resolved.isNotFound()) {
            setStatus(webRequest, HttpServletResponse.SC_NOT_FOUND);
        }
        return resolved.getViewName();
    }

    private void setStatus(NativeWebRequest webRequest, int status) {
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response != null && !response.isCommitted()) {
            response.setStatus(status);
        }
    }
}
