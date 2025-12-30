package cn.org.autumn.modules.spm.interceptor;

import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.utils.InterceptorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class SpmInterceptor extends HandlerInterceptorAdapter implements InterceptorHandler {

    @Autowired
    SuperPositionModelService superPositionModelService;

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        if (InterceptorUtils.skip(handler, this.getClass())) {
            return;
        }
        if (null != modelAndView) {
            ModelMap modelMap = modelAndView.getModelMap();
            Map<String, String> smps = superPositionModelService.getSpmListForHtml();
            modelMap.put("spm", smps);
        }
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }
}
