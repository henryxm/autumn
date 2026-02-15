package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.config.Config;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.table.TableInit;
import cn.org.autumn.table.data.InitType;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.service.MysqlTableService;
import cn.org.autumn.utils.R;
import cn.org.autumn.site.TableFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重建数据表：列出 @Table 实体、按 InitType 执行单表或全部重建
 */
@RestController
@RequestMapping("sys/reinit")
@SkipInterceptor
public class ReinitController {

    @Autowired
    private MysqlTableService mysqlTableService;

    @Autowired
    private TableInit tableInit;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @Autowired
    private TableFactory tableFactory;

    private boolean reinit = Config.isDev();

    public boolean isReinit() {
        return reinit;
    }

    public void setReinit(boolean reinit) {
        this.reinit = reinit;
    }

    /**
     * 获取所有 @Table 实体列表及基本属性、列信息
     */
    @GetMapping("/list")
    public R list(HttpServletRequest request) {
        ipWhiteService.check(request, getClass(), "reinit");
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        List<Map<String, Object>> list = mysqlTableService.listTableEntities();
        return R.ok().put("list", list);
    }

    /**
     * 执行建表：按 InitType 重建单个、多个或全部表。
     * 请求体：{ classFullNames?: string[], initType: "create"|"update"|"none" }
     * - classFullNames 为空或缺失：对所有 @Table 实体执行
     * - initType 必填
     */
    @PostMapping("/create")
    public R create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!reinit)
            return R.ok().put("msg", "未执行任何操作");
        ipWhiteService.check(request, getClass(), "reinit");
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        Object typeObj = body.get("initType");
        if (typeObj == null || StringUtils.isBlank(typeObj.toString())) {
            return R.error(400, "initType 不能为空，可选：create、update、none");
        }
        InitType initType;
        try {
            initType = InitType.valueOf(String.valueOf(typeObj).trim().toLowerCase());
        } catch (Exception e) {
            return R.error(400, "initType 不合法，可选：create、update、none");
        }
        if (initType == InitType.create) {
            return R.ok().put("msg", "仅支持:update");
        }
        if (initType == InitType.none) {
            return R.ok().put("msg", "initType 为 none，未执行任何操作");
        }
        List<Class<?>> toRun = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) body.get("classFullNames");
        if (names != null && !names.isEmpty()) {
            for (String n : names) {
                if (StringUtils.isBlank(n)) continue;
                try {
                    toRun.add(Class.forName(n.trim()));
                } catch (ClassNotFoundException e) {
                    return R.error(400, "类不存在: " + n);
                }
            }
        } else {
            Set<Class<?>> classes = mysqlTableService.getClasses();
            for (Class<?> c : classes) {
                TableInfo ti = new TableInfo(c);
                if (ti.isValid()) toRun.add(c);
            }
        }
        int done = 0;
        StringBuilder err = new StringBuilder();
        for (Class<?> clazz : toRun) {
            try {
                tableInit.create(clazz, initType);
                done++;
            } catch (Exception e) {
                if (err.length() > 0) err.append("; ");
                err.append(clazz.getName()).append(": ").append(e.getMessage());
            }
        }
        if (err.length() > 0) {
            return R.error(500, "已处理 " + done + " 个，失败: " + err.toString());
        }
        return R.ok().put("msg", "已成功处理 " + done + " 个表");
    }

    /**
     * 执行系统默认初始化：调用 tableFactory.reinit()，遍历 TableHandler 执行 reinit
     */
    @PostMapping("/defaultInit")
    public R defaultInit(HttpServletRequest request) {
        ipWhiteService.check(request, getClass(), "reinit");
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        String data = tableFactory.reinit();
        return R.ok().put("data", data != null ? data : "");
    }
}
