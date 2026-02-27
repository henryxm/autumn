package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysLogService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.site.WallDefault;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.site.PluginFactory;
import cn.org.autumn.thread.TagTaskExecutor;
import org.apache.commons.lang3.StringUtils;
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
    WebAuthenticationService webAuthenticationService;

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

    List<String> active = new ArrayList<>();

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
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("login.html")
    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.login(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("login")
    public String loginOauth(HttpServletRequest request, HttpServletResponse httpServletResponse, Model model) {
        Enumeration<String> enumeration = request.getParameterNames();
        if (!enumeration.hasMoreElements()) {
            model.addAttribute("url", "/login?redirect=login");
            return "direct";
        }
        String host = request.getHeader("host");
        WebAuthenticationEntity webAuthenticationEntity = webAuthenticationService.getByClientId(host);
        if (StringUtils.isNotEmpty(host)) {
            if (null == webAuthenticationEntity)
                webAuthenticationEntity = webAuthenticationService.getByClientId(sysConfigService.getOauth2LoginClientId(host));
            if (null != webAuthenticationEntity) {
                StringBuilder sb = new StringBuilder();
                if (StringUtils.isNotEmpty(webAuthenticationEntity.getAuthorizeUri()) && StringUtils.isNotEmpty(webAuthenticationEntity.getRedirectUri())) {
                    sb.append(webAuthenticationEntity.getAuthorizeUri());
                    sb.append("?response_type=code&client_id=");
                    sb.append(webAuthenticationEntity.getClientId());
                    sb.append("&redirect_uri=");
                    sb.append(webAuthenticationEntity.getRedirectUri());
                    String redirect = sb.toString();
                    return "redirect:" + redirect;
                }
            }
        }
        return pageFactory.login(request, httpServletResponse, model);
    }

    @RequestMapping("main.html")
    public String _main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return pageFactory.main(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("loading.html")
    @SkipInterceptor
    public String loading(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
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
    public String scan(Model model) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return "404";
        model.addAttribute("url", "/");
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