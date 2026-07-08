package cn.org.autumn.modules.opc.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.modules.opc.service.ConnectBindManageService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.R;
import java.util.Map;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 接入绑定友好管理页 API（登录用户；管理员可管理全站，普通用户仅看/解绑自己的绑定）。 */
@Slf4j
@RestController
@RequestMapping(OpcConstants.CONNECTBIND_MANAGE_API)
@SkipInterceptor
public class ConnectBindManageController {

    @Autowired
    private ConnectBindManageService connectBindManageService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @GetMapping("/overview")
    public R overview(HttpServletRequest request) {
        return execute(request, () -> {
            Viewer viewer = viewer();
            return R.ok().put("data", connectBindManageService.manageOverview(viewer.userUuid, viewer.admin));
        });
    }

    @GetMapping("/apps")
    public R apps(HttpServletRequest request) {
        return execute(request, () -> {
            Viewer viewer = viewer();
            return R.ok().put("list", connectBindManageService.listAppBriefsForManagePage(viewer.userUuid, viewer.admin));
        });
    }

    @GetMapping("/binds")
    public R binds(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        return execute(request, () -> {
            Viewer viewer = viewer();
            return R.ok().put("page", connectBindManageService.pageBindsManageViews(viewer.userUuid, viewer.admin, params));
        });
    }

    @PostMapping("/bind/create")
    public R createBind(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return execute(request, () -> {
            Viewer viewer = viewer();
            connectBindManageService.createBindForViewer(
                    viewer.userUuid,
                    viewer.admin,
                    body == null ? null : body.get("connectApp"),
                    body == null ? null : body.get("localUser"),
                    body == null ? null : body.get("openId"),
                    body == null ? null : body.get("unionId"));
            return R.ok();
        });
    }

    @PostMapping("/bind/delete")
    public R deleteBind(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return execute(request, () -> {
            Viewer viewer = viewer();
            Long id = null;
            if (body != null && body.get("id") != null) {
                id = Long.parseLong(body.get("id").toString());
            }
            connectBindManageService.deleteBindForViewer(id, viewer.userUuid, viewer.admin);
            return R.ok();
        });
    }

    private R execute(HttpServletRequest request, Supplier<R> action) {
        try {
            if (!ShiroUtils.isLogin()) {
                return R.error(401, "请先登录");
            }
            return action.get();
        } catch (Exception e) {
            log.error("ConnectBind manage API failed: {}", e.getMessage(), e);
            return R.error(e.getMessage());
        }
    }

    private Viewer viewer() {
        String userUuid = ShiroUtils.getUserUuid();
        boolean admin = sysUserRoleService.isSystemAdministrator(userUuid);
        return new Viewer(userUuid, admin);
    }

    private static final class Viewer {
        private final String userUuid;
        private final boolean admin;

        private Viewer(String userUuid, boolean admin) {
            this.userUuid = userUuid;
            this.admin = admin;
        }
    }
}
