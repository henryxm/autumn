package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.exception.AException;
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
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.sys.support.ClearTokenSupport;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.modules.wall.site.WallDefault;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.site.AuthPageSupport;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.site.PluginFactory;
import cn.org.autumn.site.SitePortalSupport;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.utils.WebPathUtils;
import org.apache.commons.lang3.StringUtils;
import cn.org.autumn.model.SecurityOpsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

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
    IpAdminWhiteService ipAdminWhiteService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    TagTaskExecutor tagTaskExecutor;

    @Autowired
    AuthPageSupport authPageSupport;

    @Autowired
    SitePortalSupport sitePortalSupport;

    @Autowired
    AdminAccessGuard adminAccessGuard;

    @RequestMapping(OpcConstants.CONNECTBIND_MANAGE_PAGE)
    @SkipInterceptor
    public String connectbindManage(HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!ShiroUtils.isLogin()) {
            return "redirect:/login";
        }
        return adminAccessGuard.gateView("opc/connectbind", request, response, model);
    }

    @RequestMapping("modules/{module}/{url}")
    public String module(@PathVariable("module") String module, @PathVariable("url") String url, String lang,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        if (StringUtils.isNotBlank(lang)) {
            model.addAttribute("locale", lang);
        }
        String view = "/modules/" + module + "/" + url;
        sitePortalSupport.applyToModelForView(request, model, view);
        return adminAccessGuard.gateView(view, request, response, model);
    }

    @RequestMapping("modules/{module}/{url}.js")
    public String js(@PathVariable("module") String module, @PathVariable("url") String url, String lang,
                     HttpServletRequest request, HttpServletResponse response, Model model) {
        if (StringUtils.isNotBlank(lang)) {
            model.addAttribute("locale", lang);
        }
        String view = "/modules/" + module + "/" + url + ".js";
        String gated = adminAccessGuard.gateView("/modules/" + module + "/" + url, request, response, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        return view;
    }

    @RequestMapping({"modules/**", "pages/**"})
    public String modules(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String view = httpServletRequest.getRequestURI();
        return adminAccessGuard.gateView(view, httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("docs/index.html")
    public String docs(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "/modules/docs/index";
    }

    @RequestMapping(value = {"index.html"})
    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        if (!ShiroUtils.isLogin()) {
            if (ShiroUtils.needLogin()) {
                return "redirect:" + WebPathUtils.forBrowser(httpServletRequest, "/login");
            }
            return pageFactory._404(httpServletRequest, httpServletResponse, model);
        }
        if (superPositionModelService.menuWithSpm()) {
            String resourceId = superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
            sitePortalSupport.applyToModelForView(httpServletRequest, model, resourceId);
            return adminAccessGuard.gateView(resourceId, httpServletRequest, httpServletResponse, model);
        }
        String indexView = pageFactory.index(httpServletRequest, httpServletResponse, model);
        return adminAccessGuard.gateView(indexView, httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("index1.html")
    public String index1(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (!ShiroUtils.isLogin()) {
            if (ShiroUtils.needLogin()) {
                return "redirect:" + WebPathUtils.forBrowser(httpServletRequest, "/login");
            }
            return pageFactory._404(httpServletRequest, httpServletResponse, model);
        }
        sitePortalSupport.applyToModel(httpServletRequest, model);
        return adminAccessGuard.gateView("index1", httpServletRequest, httpServletResponse, model);
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

    @RequestMapping({"siteportal.html"})
    @SkipInterceptor
    public String siteportal(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("siteportal", request, response, model);
    }

    @RequestMapping("main.html")
    public String _main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String mainView = pageFactory.main(httpServletRequest, httpServletResponse, model);
        return adminAccessGuard.gateView(mainView, httpServletRequest, httpServletResponse, model);
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
    public Object plugin(HttpServletRequest httpServletRequest) {
        adminAccessGuard.requireOpsAdmin(httpServletRequest, getClass(), "plugin");
        String unload = httpServletRequest.getParameter("load");
        if (StringUtils.isNotBlank(unload) && unload.equals("unload")) {
            adminAccessGuard.auditWithIp(httpServletRequest, "plugin.unload", "");
            return pluginFactory.uninstallPlugin();
        }
        adminAccessGuard.auditWithIp(httpServletRequest, "plugin.load", "");
        return pluginFactory.installPlugin();
    }

    @RequestMapping({"classpath.html", "classpath"})
    @ResponseBody
    @SkipInterceptor
    public String getClassPath(HttpServletRequest request) {
        adminAccessGuard.requireOpsAdmin(request, getClass(), "classpath");
        return System.getProperty("java.class.path");
    }

    @RequestMapping({"firewall.html", "firewall"})
    @ResponseBody
    @SkipInterceptor
    public Object firewallApi(Boolean open, Boolean white, Boolean black, Boolean host, Boolean visit, Boolean url,
                              HttpServletRequest request) {
        adminAccessGuard.requireOpsAdmin(request, getClass(), "firewall");
        // 关闭 IP 白名单须 clearToken 二次确认，避免盗用会话后关掉运维门禁
        if (Boolean.FALSE.equals(white)) {
            assertWallDangerousOpsToken(request);
        }
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
        adminAccessGuard.auditWithIp(request, "firewall.update",
                "open=" + open + ",white=" + white + ",black=" + black);
        return wallDefault;
    }

    @RequestMapping(value = "threading", method = RequestMethod.POST)
    @ResponseBody
    @SkipInterceptor
    public Map<String, Object> postThreading(HttpServletRequest request) {
        adminAccessGuard.requireOpsAdmin(request, getClass(), "threading");
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
    public String getThreading(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("thread", request, response, model);
    }

    @RequestMapping({"functionqueue.html"})
    @SkipInterceptor
    public String getFunctionQueue(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("functionqueue", request, response, model);
    }

    @RequestMapping({"logger.html"})
    @SkipInterceptor
    public String logger(Model model, HttpServletRequest request, HttpServletResponse response) {
        String gated = adminAccessGuard.gateView("logger", request, response, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        model.addAttribute("data", sysLogService.recent());
        return "logger";
    }

    @RequestMapping({"log.html"})
    @SkipInterceptor
    public String log(Model model, HttpServletRequest request, HttpServletResponse response) {
        String gated = adminAccessGuard.gateView("log", request, response, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        model.addAttribute("data", sysLogService.recent());
        return "log";
    }

    @RequestMapping({"redis.html"})
    @SkipInterceptor
    public String redis(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("redis", request, response, model);
    }

    @RequestMapping({"exec.html"})
    @SkipInterceptor
    public String exec(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("exec", request, response, model);
    }

    @RequestMapping({"cache.html"})
    @SkipInterceptor
    public String cache(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("cache", request, response, model);
    }

    @RequestMapping({"queue.html"})
    @SkipInterceptor
    public String queue(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("queue", request, response, model);
    }

    @RequestMapping({"database.html"})
    @SkipInterceptor
    public String database(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("database", request, response, model);
    }

    @RequestMapping({"dbmanage.html"})
    @SkipInterceptor
    public String dbmanage(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("dbmanage", request, response, model);
    }

    @RequestMapping({"tablescan.html"})
    @SkipInterceptor
    public String tablescan(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("tablescan", request, response, model);
    }

    @RequestMapping({OplConstants.MANAGE_PAGE})
    @SkipInterceptor
    public String oplmanage(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("opl/oplmanage", request, response, model);
    }

    @RequestMapping({OpcConstants.MANAGE_PAGE})
    @SkipInterceptor
    public String opcmanage(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("opc/opcmanage", request, response, model);
    }

    @RequestMapping({OauthAdminConstants.MANAGE_AS_PAGE})
    @SkipInterceptor
    public String oauthasmanage(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("oauth/oauthasmanage", request, response, model);
    }

    @RequestMapping({"authscopemanage.html"})
    @SkipInterceptor
    public String authscopemanage(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("auth/authscopemanage", request, response, model);
    }

    @RequestMapping({OauthRpAdminConstants.MANAGE_RP_PAGE})
    @SkipInterceptor
    public String oauthrpmanage(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("client/oauthrpmanage", request, response, model);
    }

    @RequestMapping({"loopjob.html"})
    @SkipInterceptor
    public String loopjob(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("loopjob", request, response, model);
    }

    @RequestMapping({"cluster.html"})
    @SkipInterceptor
    public String cluster(Model model, HttpServletRequest request, HttpServletResponse response) {
        return adminAccessGuard.gateView("cluster", request, response, model);
    }

    @RequestMapping({"loginlog.html"})
    @SkipInterceptor
    public String loginlog(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("loginlog", request, response, model);
    }

    @RequestMapping({"session.html"})
    @SkipInterceptor
    public String session(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("session", request, response, model);
    }

    @RequestMapping(value = {"scan.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String scan(Model model, HttpServletRequest request, HttpServletResponse response) {
        String gated = adminAccessGuard.gateView("scan", request, response, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        model.addAttribute("url", WebPathUtils.forBrowser(request, "/"));
        return "scan";
    }

    @RequestMapping(value = {"shield.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String shield(HttpServletRequest request, HttpServletResponse response, Model model) {
        return adminAccessGuard.gateView("shield", request, response, model);
    }

    @RequestMapping(value = {"clear.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String clearPage(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String gated = adminAccessGuard.gateView("clear", httpServletRequest, httpServletResponse, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        return pageFactory.clear(httpServletRequest, httpServletResponse, model);
    }

    @ResponseBody
    @RequestMapping(value = {"clear"}, method = RequestMethod.POST)
    @SkipInterceptor
    public String clearPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) throws Exception {
        adminAccessGuard.requireOpsAdmin(httpServletRequest, getClass(), "clear");
        check(httpServletRequest, "clear");
        pageFactory.clear(httpServletRequest, httpServletResponse, model);
        return "404";
    }

    @RequestMapping(value = {"reinit.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String reinit(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String gated = adminAccessGuard.gateView("reinit", httpServletRequest, httpServletResponse, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        return pageFactory.reinit(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping(value = {"fieldencrypt.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String fieldencrypt(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String gated = adminAccessGuard.gateView("fieldencrypt", httpServletRequest, httpServletResponse, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        return pageFactory.fieldencrypt(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping(value = {"configsecure.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String configsecure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String gated = adminAccessGuard.gateView("configsecure", httpServletRequest, httpServletResponse, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        return pageFactory.configsecure(httpServletRequest, httpServletResponse, model);
    }

    @ResponseBody
    @RequestMapping(value = {"reinit"}, method = RequestMethod.POST)
    @SkipInterceptor
    public String reinitPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) throws Exception {
        adminAccessGuard.requireOpsAdmin(httpServletRequest, getClass(), "reinit");
        check(httpServletRequest, "reinit");
        pageFactory.reinit(httpServletRequest, httpServletResponse, model);
        return "404";
    }

    @RequestMapping(value = {"wall.html"}, method = RequestMethod.GET)
    @SkipInterceptor
    public String wallPage(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        String gated = adminAccessGuard.gateView("wall", httpServletRequest, httpServletResponse, model);
        if ("404".equals(gated) || (gated != null && gated.contains("404"))) {
            return gated;
        }
        return pageFactory.wall(httpServletRequest, httpServletResponse, model);
    }

    public void check(HttpServletRequest request, String method) throws Exception {
        ipAdminWhiteService.checkAdmin(request, getClass(), "check");
        String token = request.getHeader("x-" + method + "-authentication");
        if (StringUtils.isBlank(token)) {
            throw new Exception("Unauthenticated");
        }
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = "clear".equals(method)
                ? ops.getClearToken()
                : ("reinit".equals(method) ? ops.getReinitToken() : null);
        if (StringUtils.isNotBlank(configured)) {
            if (!configured.equals(token)) {
                throw new Exception("Unauthenticated");
            }
            return;
        }
        // 未在 SysConfig 配置高熵运维 Token 时拒绝（后台「安全加固配置」填写）
        throw new Exception("Unauthenticated");
    }

    /** 关白名单 / 删当前 IP 等危险操作：复用 clearToken（头 x-clear-authentication）。 */
    public void assertWallDangerousOpsToken(HttpServletRequest request) {
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = ops != null ? ops.getClearToken() : null;
        String token = request.getHeader("x-clear-authentication");
        String clientKey = "page:" + StringUtils.defaultString(IPUtils.getIp(request), "-");
        try {
            ClearTokenSupport.assertMatches(configured, token, clientKey);
        } catch (AException e) {
            throw new AException("需要运维 clear Token 确认", 403);
        }
    }
}