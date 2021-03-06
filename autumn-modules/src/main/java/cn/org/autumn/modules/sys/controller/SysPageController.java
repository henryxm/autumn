package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class SysPageController {

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    WebAuthenticationService webAuthenticationService;

    @Autowired
    SysConfigService sysConfigService;

    @RequestMapping("modules/{module}/{url}")
    public String module(@PathVariable("module") String module, @PathVariable("url") String url) {
        return "modules/" + module + "/" + url;
    }

    @RequestMapping("modules/{module}/{url}.js")
    public String js(@PathVariable("module") String module, @PathVariable("url") String url) {
        return "modules/" + module + "/" + url + ".js";
    }

    @RequestMapping(value = {"index.html"})
    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        if (superPositionModelService.menuWithSpm()) {
            return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
        }
        return "index";
    }

    @RequestMapping("index1.html")
    public String index1() {
        return "index1";
    }

    @RequestMapping("login.html")
    public String login() {
        return "login";
    }

    @RequestMapping("login")
    public String loginOauth() {
        /**
         * 根据系统配置方式进行登录验证
         */
        String clientId = sysConfigService.getOauth2LoginClientId();
        if (StringUtils.isNotEmpty(clientId)) {
            WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.findByClientId(clientId);
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
        return "login";
    }

    @RequestMapping("main.html")
    public String main() {
        return "main";
    }

    @RequestMapping("404.html")
    public String notFound() {
        return "404";
    }

}
