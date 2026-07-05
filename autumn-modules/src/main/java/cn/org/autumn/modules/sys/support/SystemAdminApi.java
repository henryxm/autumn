package cn.org.autumn.modules.sys.support;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.R;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 系统管理员 REST API 统一鉴权与异常包装。 */
@Slf4j
@Component
public class SystemAdminApi {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    public R execute(HttpServletRequest request, String logTag, Supplier<R> action) {
        try {
            if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
                return R.error(403, "无权限");
            }
            return action.get();
        } catch (Exception e) {
            log.error("{} admin API failed: {}", logTag, e.getMessage(), e);
            return R.error(e.getMessage());
        }
    }
}
