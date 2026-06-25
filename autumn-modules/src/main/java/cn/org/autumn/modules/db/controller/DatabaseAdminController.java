package cn.org.autumn.modules.db.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.db.service.DatabaseAdminService;
import cn.org.autumn.modules.sys.entity.SystemUpgrade;
import cn.org.autumn.modules.sys.service.CrudGuardService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.utils.R;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 数据库综合管理 API（系统管理员）。
 */
@Slf4j
@RestController
@RequestMapping("/db/admin")
@SkipInterceptor
public class DatabaseAdminController {

    @Autowired
    private DatabaseAdminService databaseAdminService;

    @Autowired
    private CrudGuardService crudGuardService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @GetMapping("/overview")
    public R overview(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", databaseAdminService.overview()));
    }

    @GetMapping("/tables")
    public R tables(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", databaseAdminService.listManagedTables()));
    }

    @GetMapping("/crud")
    public R crud(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", databaseAdminService.crudStatus()));
    }

    @PostMapping("/crud")
    public R updateCrud(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return admin(request, () -> {
            boolean databaseWrite = parseBoolean(body.get("databaseWrite"), true);
            boolean localWrite = parseBoolean(body.get("localWrite"), true);
            String description = body.get("description") != null ? body.get("description").toString() : null;
            SystemUpgrade status = crudGuardService.update(databaseWrite, localWrite, description);
            return R.ok("写库设置已更新").put("data", status);
        });
    }

    @PostMapping("/crud/reload")
    public R reloadCrud(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", crudGuardService.resetToConfig()));
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        try {
            ipWhiteService.check(request, getClass(), "dbmanage");
            if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return R.error(403, "无权限");
            }
            return action.get();
        } catch (Exception e) {
            log.error("Database admin API failed:{}", e.getMessage(), e);
            return R.error(e.getMessage());
        }
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = value.toString().trim();
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}
