package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.site.PluginFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class SysPageController implements ErrorController {

    @Autowired
    @Lazy
    SuperPositionModelService superPositionModelService;

    @Autowired
    @Lazy
    WebAuthenticationService webAuthenticationService;

    @Autowired
    @Lazy
    SysConfigService sysConfigService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    PluginFactory pluginFactory;

    @RequestMapping("modules/{module}/{url}")
    public String module(@PathVariable("module") String module, @PathVariable("url") String url) {
        return "modules/" + module + "/" + url;
    }

    @RequestMapping("modules/{module}/{url}.js")
    public String js(@PathVariable("module") String module, @PathVariable("url") String url) {
        return "modules/" + module + "/" + url + ".js";
    }

    @RequestMapping({"modules/**", "pages/**"})
    public String modules(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getRequestURI();
    }

    @RequestMapping(value = {"index.html"})
    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        if (superPositionModelService.menuWithSpm()) {
            return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
        }
        return pageFactory.index(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("index1.html")
    public String index1(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "index1";
    }

    @RequestMapping("login.html")
    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.login(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("login")
    public String loginOauth(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        /**
         * 根据系统配置方式进行登录验证
         */
        String host = httpServletRequest.getHeader("host");
        String clientId = sysConfigService.getOauth2LoginClientId(host);
        if (StringUtils.isNotEmpty(clientId)) {
            WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.getByClientId(clientId);
            if (null != webAuthenticationEntity) {
                StringBuffer sb = new StringBuffer();
                if (StringUtils.isNotEmpty(webAuthenticationEntity.getAuthorizeUri()) && StringUtils.isNotEmpty(webAuthenticationEntity.getRedirectUri())) {
                    sb.append(webAuthenticationEntity.getAuthorizeUri());
                    sb.append("?response_type=code&client_id=" + clientId + "&redirect_uri=");
                    sb.append(webAuthenticationEntity.getRedirectUri());
                    String redirect = sb.toString();
                    return "redirect:" + redirect;
                }
            }
        }
        return pageFactory.login(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("main.html")
    public String main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.main(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("loading.html")
    public String loading(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.loading(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"404.html", "404"})
    public String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"500.html", "500"})
    public String _500(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory._500(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"505.html", "505"})
    public String _505(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory._505(httpServletRequest, httpServletResponse, model);
    }

    @Override
    public String getErrorPath() {
        return null;
    }

    @RequestMapping({"error.html", "error"})
    public String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.error(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"plugin.html", "plugin"})
    @ResponseBody
    public Object plugin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String unload = httpServletRequest.getParameter("load");
        if (StringUtils.isNotBlank(unload) && unload.equals("unload")) {
            return pluginFactory.uninstallPlugin();
        } else {
            return pluginFactory.installPlugin();
        }
    }
}