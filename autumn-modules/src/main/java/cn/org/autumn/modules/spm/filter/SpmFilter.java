package cn.org.autumn.modules.spm.filter;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.wall.service.WallService;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

public class SpmFilter extends FormAuthenticationFilter {

    protected boolean isEnabled(ServletRequest request, ServletResponse response) {
        WallService wallService = (WallService) Config.getBean("wallService");
        return wallService.isEnabled(request, response,false);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (null != request) {
            String spm = httpServletRequest.getParameter("spm");
            SuperPositionModelService superPositionModelService = (SuperPositionModelService) Config.getBean("superPositionModelService");
            if (null != superPositionModelService && !superPositionModelService.needLogin(httpServletRequest, spm))
                return true;
        }
        return super.isAccessAllowed(request, response, mappedValue);
    }
}
