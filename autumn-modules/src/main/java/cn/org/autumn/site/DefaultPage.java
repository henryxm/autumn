package cn.org.autumn.site;

import cn.org.autumn.config.PageHandler;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.WebPathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Integer.MAX_VALUE / 10)
public class DefaultPage implements PageHandler {

    @Autowired
    SysConfigService sysConfigService;

    @Override
    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        boolean oauthLogin = StringUtils.isNotBlank(sysConfigService.getOauth2LoginClientId());
        model.addAttribute("oauthLogin", oauthLogin);
        if (!model.containsAttribute("bodyClass")) {
            model.addAttribute("bodyClass", "login-page-v2");
        }
        if (!model.containsAttribute("error")) {
            String error = httpServletRequest.getParameter("error");
            if (StringUtils.isNotBlank(error)) {
                model.addAttribute("error", error);
            }
        }
        AuthPageAttributes.apply(model, sysConfigService);
        return "login";
    }

    @Override
    public String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return WebPathUtils.forBrowser(httpServletRequest, "/");
    }

    @Override
    public String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(404);
        return "404";
    }

    @Override
    public String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(500);
        return "error";
    }

    @Override
    public String header(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "header";
    }

    @Override
    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "index";
    }

    @Override
    public String main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "main";
    }

    @Override
    public String scan(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        model.addAttribute("url", WebPathUtils.forBrowser(httpServletRequest, "/"));
        return "scan";
    }
}