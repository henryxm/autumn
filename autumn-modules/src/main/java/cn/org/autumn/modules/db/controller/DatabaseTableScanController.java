package cn.org.autumn.modules.db.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.db.service.DatabaseTableScanService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全表特征值扫描 API（系统管理员）。
 */
@Slf4j
@RestController
@RequestMapping("/db/admin/tablescan")
@SkipInterceptor
public class DatabaseTableScanController {

    @Autowired
    private DatabaseTableScanService databaseTableScanService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @PostMapping("/search")
    public R search(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return admin(request, () -> {
            String columnName = stringValue(body.get("columnName"));
            String value = stringValue(body.get("value"));
            Integer previewLimit = intValue(body.get("previewLimit"));
            return R.ok().put("data", databaseTableScanService.search(columnName, value, previewLimit));
        });
    }

    @GetMapping("/preview")
    public R preview(@RequestParam("table") String table,
                     @RequestParam("column") String column,
                     @RequestParam("value") String value,
                     @RequestParam(value = "limit", required = false) Integer limit,
                     HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", databaseTableScanService.preview(table, column, value, limit)));
    }

    @GetMapping("/columns")
    public R columns(@RequestParam("columnName") String columnName, HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", databaseTableScanService.listCandidates(columnName)));
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        try {
            ipWhiteService.check(request, getClass(), "tablescan");
            if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return R.error(403, "无权限");
            }
            return action.get();
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("Table scan API failed:{}", e.getMessage(), e);
            return R.error(e.getMessage());
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }
}
