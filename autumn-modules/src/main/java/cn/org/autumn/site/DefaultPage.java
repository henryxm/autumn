package cn.org.autumn.site;

import cn.org.autumn.config.PageHandler;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.WebPathUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@Order(Integer.MAX_VALUE / 10)
public class DefaultPage implements PageHandler {

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    AuthPageSupport authPageSupport;

    @Override
    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        boolean oauthAuthorize = model != null && Boolean.TRUE.equals(model.getAttribute(AuthPageAttributes.ATTR_OAUTH_AUTHORIZE));
        boolean oplAuthorize = model != null && Boolean.TRUE.equals(model.getAttribute(AuthPageAttributes.ATTR_OPL_AUTHORIZE));
        if (oauthAuthorize) {
            return oauthAuthorize(httpServletRequest, httpServletResponse, model);
        }
        if (oplAuthorize) {
            return oplAuthorize(httpServletRequest, httpServletResponse, model);
        }
        authPageSupport.prepareAuthorizePage(httpServletRequest, model, false, false);
        return "login";
    }

    @Override
    public String register(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        AuthPageAttributes.apply(model, sysConfigService);
        return "register";
    }

    @Override
    public String forgotPassword(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        AuthPageAttributes.apply(model, sysConfigService);
        return "forgotpassword";
    }

    @Override
    public String oauthAuthorize(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        authPageSupport.prepareOauthAuthorize(httpServletRequest, model);
        return "login";
    }

    @Override
    public String oplAuthorize(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        authPageSupport.prepareOplAuthorize(httpServletRequest, model);
        return "login";
    }

    @Override
    public String oauthLoginEntry(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "oauth2/login";
    }

    @Override
    public String oauthLoginSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "oauth2/success";
    }

    @Override
    public String openLoginEntry(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "open/oauth2/login";
    }

    @Override
    public String openLoginSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "open/oauth2/success";
    }

    @Override
    public String authCallbackError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        AuthPageAttributes.apply(model, sysConfigService);
        AuthPageAttributes.markFlowKind(model, AuthPageAttributes.FLOW_AUTH_CALLBACK_ERROR);
        AuthPageAttributes.applyAuthFlowBoot(httpServletRequest, model);
        return "oauth2/callback-error";
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
