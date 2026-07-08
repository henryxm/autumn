package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.modules.client.support.OauthRpAdminConstants;
import cn.org.autumn.modules.oauth.support.OauthAdminConstants;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysLogService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.site.WallDefault;
import cn.org.autumn.site.AuthPageAttributes;
import cn.org.autumn.site.AuthPageSupport;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.site.PluginFactory;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.utils.WebPathUtils;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SysPageController implements ErrorController {

    @Autowired
    @Lazy
    SuperPositionModelService superPositionModelService;

    @Autowired
    @Lazy
    SysConfigService sysConfigService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    PluginFactory pluginFactory;

    @Autowired
    WallDefault wallDefault;

    @Autowired
    SysUserRoleService sysUserRoleService;

    @Autowired
    IpWhiteService ipWhiteService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    TagTaskExecutor tagTaskExecutor;

    @Autowired
    AuthPageSupport authPageSupport;

    List<String> active = new ArrayList<>();

    @RequestMapping(OpcConstants.CONNECTBIND_MANAGE_PAGE)
    @SkipInterceptor
    public String connectbindManage() {
        if (!ShiroUtils.isLogin()) {
            return "redirect:/login";
        }
        return "opc/connectbind";
    }

    @RequestMapping("modules/{module}/{url}")
    public String module(@PathVariable("module") String module, @PathVariable("url") String url, String lang, Model model) {
        if (StringUtils.isNotBlank(lang)) {
            model.addAttribute("locale", lang);
        }
        return "/modules/" + module + "/" + url;
    }

    @RequestMapping("modules/{module}/{url}.js")
    public String js(@PathVariable("module") String module, @PathVariable("url") String url, String lang, Model model) {
        if (StringUtils.isNotBlank(lang)) {
            model.addAttribute("locale", lang);
        }
        return "/modules/" + module + "/" + url + ".js";
    }

    @RequestMapping({"modules/**", "pages/**"})
    public String modules(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getRequestURI();
    }

    @RequestMapping("docs/index.html")
    public String docs(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "/modules/docs/index";
    }

    @RequestMapping(value = {"index.html"})
    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        active(httpServletRequest);
        inactive(httpServletRequest);
        if (isActive(httpServletRequest)) {
            if (superPositionModelService.menuWithSpm()) {
                return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
            }
            return pageFactory.index(httpServletRequest, httpServletResponse, model);
        }
        if (ShiroUtils.needLogin()) {
            return "redirect:" + WebPathUtils.forBrowser(httpServletRequest, "/login");
        }
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    public void active(HttpServletRequest request) {
        String param = request.getParameter("active");
        if (StringUtils.isBlank(param) || !"admin".equals(param))
            return;
        String sessionId = request.getSession().getId();
        active.add(sessionId);
    }

    public boolean isActive(HttpServletRequest request) {
        if (ShiroUtils.isLogin()) {
            return true;
        }
        String sessionId = request.getSession().getId();
        return active.contains(sessionId);
    }

    public void inactive(HttpServletRequest request) {
        String param = request.getParameter("inactive");
        if (StringUtils.isNotBlank(param) && "admin".equals(param)) {
            String sessionId = request.getSession().getId();
            active.remove(sessionId);
        }
    }

    @RequestMapping("index1.html")
    public String index1(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (isActive(httpServletRequest))
            return "index1";
        if (ShiroUtils.needLogin()) {
            return "redirect:" + WebPathUtils.forBrowser(httpServletRequest, "/login");
        }
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"login.html", "login"})
    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.login(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"oauth2/success.html", "oauth2/success"})
    @SkipInterceptor(AuthorizationInterceptor.class)
    public String oauthLoginSuccess(HttpServletRequest request, HttpServletResponse response, Model model) {
        authPageSupport.prepareOauthLoginSuccess(request, model);
        return pageFactory.oauthLoginSuccess(request, response, model);
    }

    @RequestMapping({OpcConstants.OAUTH2_LOGIN_PAGE + ".html", OpcConstants.OAUTH2_LOGIN_PAGE})
    @SkipInterceptor(AuthorizationInterceptor.class)
    public String openLoginEntry(HttpServletRequest request, HttpServletResponse response, Model model, @RequestParam(required = false) String appId) {
        String callback = request.getParameter("callback");
        String canonicalUrl = WebPathUtils.oauthLoginEntryUrlIfCallbackNeedsCanonical(
                request, OpcConstants.OAUTH2_LOGIN_PATH, "appId", appId, callback);
        if (StringUtils.isNotBlank(canonicalUrl)) {
            return "redirect:" + canonicalUrl;
        }
        authPageSupport.prepareOpenLoginEntry(request, model, appId);
        return pageFactory.openLoginEntry(request, response, model);
    }

    @RequestMapping({OpcConstants.OAUTH2_SUCCESS_PAGE + ".html", OpcConstants.OAUTH2_SUCCESS_PAGE})
    @SkipInterceptor(AuthorizationInterceptor.class)
    public String openLoginSuccess(HttpServletRequest request, HttpServletResponse response, Model model, @RequestParam(required = false) String appId) {
        authPageSupport.prepareOpenLoginSuccess(request, model, appId);
        return pageFactory.openLoginSuccess(request, response, model);
    }

    @RequestMapping({"register.html", "register"})
    public String register(HttpServletRequest request, HttpServletResponse response, Model model) {
        return pageFactory.register(request, response, model);
    }

    @RequestMapping({"forgotpassword.html", "forgotpassword"})
    public String forgotPassword(HttpServletRequest request, HttpServletResponse response, Model model) {
        return pageFactory.forgotPassword(request, response, model);
    }

    @RequestMapping({"user/service.html", "user/service"})
    public String userService(Model model) {
        model.addAttribute("bodyClass", "login-page-v2 legal-page");
        AuthPageAttributes.apply(model, sysConfigService);
        return "user/service";
    }

    @RequestMapping({"user/privacy.html", "user/privacy"})
    public String userPrivacy(Model model) {
        model.addAttribute("bodyClass", "login-page-v2 legal-page");
        AuthPageAttributes.apply(model, sysConfigService);
        return "user/privacy";
    }

    @RequestMapping("main.html")
    public String main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.main(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("loading.html")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public String loading(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        model.addAttribute("loadingBrand", sysConfigService.getLoadingBrand());
        model.addAttribute("loadingAccent", sysConfigService.getLoadingAccent());
        model.addAttribute("loadingLogoUrl", sysConfigService.getLoadingLogoUrl());
        return pageFactory.loading(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"404.html", "404"})
    @SkipInterceptor
    public String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"500.html", "500"})
    @SkipInterceptor
    public String _500(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory._500(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"505.html", "505"})
    @SkipInterceptor
    public String _505(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory._505(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"error.html", "error"})
    @SkipInterceptor
    public String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.error(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping({"plugin.html", "plugin"})
    @ResponseBody
    @SkipInterceptor
    public Object plugin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String unload = httpServletRequest.getParameter("load");
        if (StringUtils.isNotBlank(unload) && unload.equals("unload")) {
            return pluginFactory.uninstallPlugin();
        } else {
            return pluginFactory.installPlugin();
        }
    }

    @RequestMapping({"classpath.html", "classpath"})
    @ResponseBody
    @SkipInterceptor
    public String getClassPath() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "";
        return System.getProperty("java.class.path");
    }

    @RequestMapping({"firewall.html", "firewall"})
    @ResponseBody
    @SkipInterceptor
    public WallDefault wall(Boolean open, Boolean white, Boolean black, Boolean host, Boolean visit, Boolean url) {

        if (null != open)
            wallDefault.setOpen(open);
        if (null != white)
            wallDefault.setIpWhiteEnable(white);
        if (null != black)
            wallDefault.setIpBlackEnable(black);
        if (null != host)
            wallDefault.setHostEnable(host);
        if (null != visit)
            wallDefault.setVisitEnable(visit);
        if (null != url)
            wallDefault.setUrlBlackEnable(url);
        return wallDefault;
    }

    @RequestMapping(value = "threading", method = RequestMethod.POST)
    @ResponseBody
    @SkipInterceptor
    public Map<String, Object> postThreading() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return null;
        return threading();
    }

    public Map<String, Object> threading() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> executor = new HashMap<>();
        executor.put("ActiveCount", tagTaskExecutor.getActiveCount());
        executor.put("CorePoolSize", tagTaskExecutor.getCorePoolSize());
        executor.put("MaxPoolSize", tagTaskExecutor.getMaxPoolSize());
        executor.put("PoolSize", tagTaskExecutor.getPoolSize());
        map.put("Executor", executor);
        map.put("Tags", tagTaskExecutor.getRunning());
        return map;
    }

    @RequestMapping({"threading.html"})
    @SkipInterceptor
    public String getThreading(Model model, HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "getThreading");
        return "thread";
    }

    @RequestMapping({"logger.html"})
    @SkipInterceptor
    public String logger(Model model, HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        model.addAttribute("data", sysLogService.recent());
        ipWhiteService.check(servlet, getClass(), "logger");
        return "logger";
    }

    @RequestMapping({"log.html"})
    @SkipInterceptor
    public String log(Model model, HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        model.addAttribute("data", sysLogService.recent());
        ipWhiteService.check(servlet, getClass(), "log");
        return "log";
    }

    @RequestMapping({"redis.html"})
    @SkipInterceptor
    public String redis(HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "redis");
        return "redis";
    }

    @RequestMapping({"exec.html"})
    @SkipInterceptor
    public String exec(Model model, HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "exec");
        return "exec";
    }

    @RequestMapping({"cache.html"})
    @SkipInterceptor
    public String cache(Model model) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "cache";
    }

    @RequestMapping({"queue.html"})
    @SkipInterceptor
    public String queue(Model model) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "queue";
    }

    @RequestMapping({"database.html"})
    @SkipInterceptor
    public String database(Model model) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "database";
    }

    @RequestMapping({"dbmanage.html"})
    @SkipInterceptor
    public String dbmanage(HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "dbmanage");
        return "dbmanage";
    }

    @RequestMapping({OplConstants.MANAGE_PAGE})
    @SkipInterceptor
    public String oplmanage() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "opl/oplmanage";
    }

    @RequestMapping({OpcConstants.MANAGE_PAGE})
    @SkipInterceptor
    public String opcmanage() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "opc/opcmanage";
    }

    @RequestMapping({OauthAdminConstants.MANAGE_AS_PAGE})
    @SkipInterceptor
    public String oauthasmanage() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "oauth/oauthasmanage";
    }

    @RequestMapping({OauthRpAdminConstants.MANAGE_RP_PAGE})
    @SkipInterceptor
    public String oauthrpmanage() {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "client/oauthrpmanage";
    }

    @RequestMapping({"loopjob.html"})
    @SkipInterceptor
    public String loopjob(Model model) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        return "loopjob";
    }

    @RequestMapping({"loginlog.html"})
    @SkipInterceptor
    public String loginlog(HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "loginlog");
        return "loginlog";
    }

    @RequestMapping({"session.html"})
    @SkipInterceptor
    public String session(HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "session");
        return "session";
    }

    @RequestMapping(value = {"scan.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String scan(Model model, HttpServletRequest request) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        model.addAttribute("url", WebPathUtils.forBrowser(request, "/"));
        return "scan";
    }

    @RequestMapping(value = {"shield.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String shield(HttpServletRequest servlet) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        ipWhiteService.check(servlet, getClass(), "shield");
        return "shield";
    }

    @RequestMapping(value = {"clear.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String clearPage(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (ShiroUtils.isLogin()) {
            if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return pageFactory.clear(httpServletRequest, httpServletResponse, model);
            }
        }
        return "404";
    }

    @ResponseBody
    @RequestMapping(value = {"clear"}, method = RequestMethod.POST)
    @SkipInterceptor
    public String clearPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) throws Exception {
        if (ShiroUtils.isLogin()) {
            if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                check(httpServletRequest, "clear");
                pageFactory.clear(httpServletRequest, httpServletResponse, model);
            }
        }
        return "404";
    }

    @RequestMapping(value = {"reinit.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String reinit(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        ipWhiteService.check(httpServletRequest, getClass(), "reinit");
        if (ShiroUtils.isLogin()) {
            if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return pageFactory.reinit(httpServletRequest, httpServletResponse, model);
            }
        }
        return "404";
    }

    @RequestMapping(value = {"fieldencrypt.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String fieldencrypt(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        ipWhiteService.check(httpServletRequest, getClass(), "fieldencrypt");
        if (ShiroUtils.isLogin()) {
            if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return pageFactory.fieldencrypt(httpServletRequest, httpServletResponse, model);
            }
        }
        return "404";
    }

    @ResponseBody
    @RequestMapping(value = {"reinit"}, method = RequestMethod.POST)
    @SkipInterceptor
    public String reinitPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) throws Exception {
        if (ShiroUtils.isLogin()) {
            if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                check(httpServletRequest, "reinit");
                pageFactory.reinit(httpServletRequest, httpServletResponse, model);
            }
        }
        return "404";
    }

    @RequestMapping(value = {"wall.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String wall(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (ShiroUtils.isLogin()) {
            if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return pageFactory.wall(httpServletRequest, httpServletResponse, model);
            }
        }
        return "404";
    }

    public void check(HttpServletRequest request, String method) throws Exception {
        ipWhiteService.check(request, getClass(), "check");
        String token = request.getHeader("x-" + method + "-authentication");
        if (StringUtils.isBlank(token)) {
            throw new Exception("Unauthenticated");
        }
        List<String> allowed = new ArrayList<>();
        allowed.add("application/client-" + method);
        if (!allowed.contains(token)) {
            throw new Exception("Unauthenticated");
        }
    }
}
