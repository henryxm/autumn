package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.dto.PasswordChangeLogView;
import cn.org.autumn.modules.sys.service.PasswordChangeLogService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 登录口令变更审计只读列表：不对普通用户开放；响应不含哈希/盐。
 */
@RestController
@RequestMapping("/sys/passwordlog")
public class PasswordChangeLogController {

    @Autowired
    private PasswordChangeLogService passwordChangeLogService;

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    /**
     * 分页查询口令变更元数据（操作者/目标/动作/渠道/IP/时间等）。
     * 门禁：系统管理员 + 管理 IP 白名单；不挂可分配菜单权限。
     */
    @GetMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            adminAccessGuard.requireOpsAdmin(request, getClass(), "list");
        } catch (AException e) {
            return R.error(403, "无权限");
        }
        Page<PasswordChangeLogView> page = passwordChangeLogService.queryPageViews(params);
        return R.ok().put("page", new PageUtils(page));
    }
}
