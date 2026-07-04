package cn.org.autumn.view;

import cn.org.autumn.config.ViewHandler;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.site.ViewFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * 扩展 FreeMarker 视图解析：按视图名动态调整后缀（{@link ViewHandler}），并在解析阶段二次校验模板。
 * <p>
 * 与 {@link cn.org.autumn.handler.ViewNameReturnValueHandler} 形成双保险——若返回值未经 Handler 包装
 *（如部分内部 forward），仍在此拦截缺失模板，避免进入渲染期才抛错。
 */
public class NameBasedViewResolver extends FreeMarkerViewResolver {

    private final ViewFactory viewFactory;

    private ViewTemplateSupport viewTemplateSupport;

    public NameBasedViewResolver(ViewFactory viewFactory) {
        this.viewFactory = viewFactory;
        setViewClass(NameBasedFreeMarkerView.class);
    }

    public void setViewTemplateSupport(ViewTemplateSupport viewTemplateSupport) {
        this.viewTemplateSupport = viewTemplateSupport;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if (viewName == null) {
            return null;
        }
        if (viewTemplateSupport != null) {
            String originalViewName = viewTemplateSupport.isSpecialViewName(viewName)
                    ? viewName
                    : viewTemplateSupport.normalizeViewName(viewName);
            if (viewTemplateSupport.isSpecialViewName(originalViewName)) {
                return super.resolveViewName(originalViewName, locale);
            }
            if (viewTemplateSupport.exists(originalViewName)) {
                return super.resolveViewName(originalViewName, locale);
            }
            // exists() 可能误报（多 JAR / DynamicTemplateLoader）；能解析原视图则视为存在
            View originalView = super.resolveViewName(originalViewName, locale);
            if (originalView != null) {
                return originalView;
            }
            View fallbackView = super.resolveViewName(ViewTemplateSupport.FALLBACK_404_VIEW, locale);
            if (fallbackView != null) {
                setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
                return fallbackView;
            }
            return null;
        }
        if (viewName.startsWith("/") && !viewName.startsWith("redirect:") && !viewName.startsWith("forward:")) {
            viewName = viewName.substring(1);
        }
        return super.resolveViewName(viewName, locale);
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        String resolvedName = viewName;
        if (viewTemplateSupport != null && !viewTemplateSupport.isSpecialViewName(viewName)) {
            resolvedName = viewTemplateSupport.normalizeViewName(viewName);
        }
        AbstractUrlBasedView basedView = super.buildView(resolvedName);
        if (!InstallMode.isActive()) {
            ViewHandler viewHandler = viewFactory.getShould(resolvedName);
            if (null != viewHandler) {
                String url = viewHandler.getUrl(getPrefix(), resolvedName, getSuffix());
                basedView.setUrl(url);
            }
        }
        return basedView;
    }

    private void setResponseStatus(int status) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getResponse() != null) {
                attributes.getResponse().setStatus(status);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 非 404 回落页渲染前，清除视图解析链上误设的错误状态码（Controller 已返回 200 且模板可渲染）。
     */
    static final class NameBasedFreeMarkerView extends FreeMarkerView {

        @Override
        protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            if (!isFallback404View() && !response.isCommitted()
                    && response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST) {
                response.setStatus(HttpServletResponse.SC_OK);
            }
            super.renderMergedTemplateModel(model, request, response);
        }

        private boolean isFallback404View() {
            String url = getUrl();
            if (StringUtils.isBlank(url)) {
                return false;
            }
            String normalized = url.replace('\\', '/');
            return normalized.endsWith("/404.html") || normalized.endsWith("/404")
                    || ViewTemplateSupport.FALLBACK_404_VIEW.equals(getBeanName());
        }
    }
}
