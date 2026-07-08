package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.modules.sys.service.SitePortalAdminService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.exception.ResponseThrowable;
import cn.org.autumn.utils.R;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/sys/site-portal")
@SkipInterceptor
public class SitePortalAdminController {

    @Autowired
    private SitePortalAdminService sitePortalAdminService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @GetMapping("/config")
    public R config(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", sitePortalAdminService.loadConfig()));
    }

    @GetMapping("/defaults")
    public R defaults(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", sitePortalAdminService.defaults()));
    }

    @GetMapping("/preview-psb")
    public R previewPsb(HttpServletRequest request, @RequestParam(required = false) String number) {
        return admin(request, () -> R.ok().put("url", sitePortalAdminService.previewPsbUrl(number)));
    }

    @GetMapping("/preview-filing")
    public R previewFiling(HttpServletRequest request,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String url) {
        return admin(request, () -> R.ok().put("url", sitePortalAdminService.previewFilingUrl(type, number, url)));
    }

    @PostMapping("/save")
    public R save(@RequestBody SitePortalConfig config, HttpServletRequest request) {
        return admin(request, () -> R.ok("保存成功").put("data", sitePortalAdminService.saveConfig(config)));
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        try {
            ipWhiteService.check(request, getClass(), "siteportal");
            if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return R.error(403, "无权限");
            }
            return action.get();
        } catch (Exception e) {
            if (e instanceof ResponseThrowable) {
                ResponseThrowable rejected = (ResponseThrowable) e;
                log.warn("Site portal admin API rejected: {}", rejected.getMsg());
                return R.error(rejected.getMsg());
            }
            log.error("Site portal admin API failed: {}", e.getMessage(), e);
            return R.error("操作失败，请稍后重试");
        }
    }
}
