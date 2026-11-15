package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.AdminBootstrapService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.utils.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 运维只读：引导口令文件是否存在 / pending 时重新展示明文（需 ops Token）。
 */
@RestController
@RequestMapping("sys/ops/admin-bootstrap")
public class AdminBootstrapController {

    public static final String HEADER = "x-bootstrap-authentication";

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @GetMapping("/status")
    @SkipInterceptor
    public R status(HttpServletRequest request) throws Exception {
        assertOps(request);
        return R.ok().put("data", adminBootstrapService.opsStatus());
    }

    @GetMapping("/reveal")
    @SkipInterceptor
    public R reveal(HttpServletRequest request) throws Exception {
        assertOps(request);
        return R.ok().put("data", adminBootstrapService.opsReveal());
    }

    private void assertOps(HttpServletRequest request) throws Exception {
        ipWhiteService.check(request, getClass(), "assertOps");
        String token = request.getHeader(HEADER);
        if (StringUtils.isBlank(token)) {
            throw new Exception("Unauthenticated");
        }
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = StringUtils.isNotBlank(ops.getClearToken()) ? ops.getClearToken() : ops.getReinitToken();
        if (StringUtils.isBlank(configured) || !configured.equals(token)) {
            throw new Exception("Unauthenticated");
        }
    }
}
