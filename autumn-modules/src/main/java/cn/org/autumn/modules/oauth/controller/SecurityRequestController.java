package cn.org.autumn.modules.oauth.controller;

import cn.org.autumn.modules.oauth.entity.SecurityRequestEntity;
import cn.org.autumn.utils.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.oauth.controller.gen.SecurityRequestControllerGen;

import java.util.List;
import java.util.Map;

/**
 * 安全验证
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-03
 */
@RestController
@RequestMapping("oauth/securityrequest")
public class SecurityRequestController extends SecurityRequestControllerGen {

    @GetMapping("/current")
    @RequiresPermissions("oauth:securityrequest:list")
    public R current() {
        SecurityRequestEntity entity = securityRequestService.ensureCurrent();
        return R.ok().put("securityRequest", entity);
    }

    @GetMapping("/verify/logs")
    @RequiresPermissions("oauth:securityrequest:list")
    public R verifyLogs(@RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
        List<Map<String, Object>> list = securityRequestService.getVerifyStrongLogs(limit);
        return R.ok().put("list", list);
    }

    @PostMapping("/verify/logs/clear")
    @RequiresPermissions("oauth:securityrequest:update")
    public R clearVerifyLogs() {
        securityRequestService.clearVerifyStrongLogs();
        return R.ok();
    }
}
