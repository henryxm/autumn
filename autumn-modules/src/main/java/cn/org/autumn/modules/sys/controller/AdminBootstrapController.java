package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.ops.service.OpsSessionService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.AdminBootstrapService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.ClearTokenSupport;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.ops.AutumnOpsProbe;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 运维只读：引导口令文件是否存在 / pending 时重新展示明文。
 * <p>
 * 须：管理 IP（若开启）+ 已配置的 clearToken/reinitToken +（运维证书会话 <strong>或</strong> 已登录超管）。
 * 运维会话不可首次铸造 clearToken，故无法单独用 OpsEnroll 走 reveal 提权链。
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
    private IpAdminWhiteService ipAdminWhiteService;

    @Autowired
    private OpsSessionService opsSessionService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

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
        ipAdminWhiteService.checkAdmin(request, getClass(), "assertOps");
        requireOpsSessionOrSystemAdmin(request);
        String token = request.getHeader(HEADER);
        if (StringUtils.isBlank(token)) {
            throw new Exception("Unauthenticated");
        }
        SecurityOpsConfig ops = sysConfigService.getSecurityOpsConfig();
        String configured = null;
        if (ops != null) {
            configured = StringUtils.isNotBlank(ops.getClearToken()) ? ops.getClearToken() : ops.getReinitToken();
        }
        if (StringUtils.isBlank(configured)) {
            throw new Exception("Unauthenticated");
        }
        String clientKey = "bootstrap:" + StringUtils.defaultString(IPUtils.getIp(request), "-");
        if (!ClearTokenSupport.matches(configured, token, clientKey)) {
            throw new Exception("Unauthenticated");
        }
    }

    private void requireOpsSessionOrSystemAdmin(HttpServletRequest request) throws Exception {
        String sid = request.getHeader(AutumnOpsProbe.SESSION_HEADER);
        if (StringUtils.isNotBlank(sid)) {
            try {
                opsSessionService.requireSession(sid);
                return;
            } catch (AException ignored) {
                // fall through to system-admin cookie session
            }
        }
        if (ShiroUtils.isLogin()
                && sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return;
        }
        throw new Exception("Unauthenticated");
    }
}
