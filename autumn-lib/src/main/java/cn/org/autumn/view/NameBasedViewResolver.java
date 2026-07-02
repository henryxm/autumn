package cn.org.autumn.view;

import cn.org.autumn.config.ViewHandler;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.site.ViewFactory;
import cn.org.autumn.view.ViewTemplateSupport.ResolvedView;
import java.util.Locale;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
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
            ResolvedView resolved = viewTemplateSupport.resolve(viewName);
            if (resolved.isNotFound()) {
                setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            viewName = resolved.getViewName();
        } else if (viewName.startsWith("/") && !viewName.startsWith("redirect:") && !viewName.startsWith("forward:")) {
            // ViewTemplateSupport 未注入时的最低保障：仍去掉前导 /，降低循环视图风险
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
        // 安装向导阶段无 sys_config 等业务表；视图名若含「.」（如协商为 *.html）会触发 NoneSuffixViewHandler 查库导致 500
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
}
