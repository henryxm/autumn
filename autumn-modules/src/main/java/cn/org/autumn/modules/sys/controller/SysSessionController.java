package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.R;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 会话管理：查询、删除单个会话、按用户强制下线
 */
@RestController
@RequestMapping("sys/session")
public class SysSessionController {

    @Autowired
    private ShiroSessionService shiroSessionService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    /**
     * 活动会话列表（分页），支持按用户 UUID、用户名、会话 ID 筛选；每页默认 20 条
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) String userUuid,
                  @RequestParam(required = false) String username,
                  @RequestParam(required = false) String sessionId,
                  @RequestParam(required = false, defaultValue = "1") int page,
                  @RequestParam(required = false, defaultValue = "20") int limit) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        String currentId = null;
        try {
            if (ShiroUtils.isLogin()) currentId = ShiroUtils.getSession().getId() != null ? ShiroUtils.getSession().getId().toString() : null;
        } catch (Exception ignored) {
        }
        Map<String, Object> pageResult = shiroSessionService.getActiveSessionList(
                StringUtils.trimToEmpty(userUuid),
                StringUtils.trimToEmpty(username),
                currentId,
                StringUtils.trimToEmpty(sessionId),
                Math.max(1, page),
                Math.max(1, Math.min(limit, 500)));
        return R.ok()
                .put("list", pageResult.get("list"))
                .put("totalCount", pageResult.get("totalCount"))
                .put("currPage", pageResult.get("currPage"))
                .put("totalPage", pageResult.get("totalPage"))
                .put("currentSessionId", currentId);
    }

    /**
     * 删除指定会话
     */
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public R delete(@RequestBody Map<String, Object> body) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        Object o = body.get("sessionId");
        if (o == null || StringUtils.isBlank(o.toString())) {
            return R.error(400, "sessionId 不能为空");
        }
        String sessionId = o.toString().trim();
        boolean ok = shiroSessionService.deleteSession(sessionId);
        return ok ? R.ok().put("msg", "已删除") : R.error(500, "删除失败或会话不存在");
    }

    /**
     * 按用户强制下线（踢掉该用户所有会话）
     */
    @RequestMapping(value = "/forceLogout", method = RequestMethod.DELETE)
    public R forceLogout(@RequestBody Map<String, Object> body) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        Object o = body.get("userUuid");
        if (o == null || StringUtils.isBlank(o.toString())) {
            return R.error(400, "userUuid 不能为空");
        }
        String userUuid = o.toString().trim();
        int count = shiroSessionService.forceLogoutByUserUuid(userUuid);
        return R.ok().put("count", count).put("msg", "已踢出 " + count + " 个会话");
    }

    /**
     * 取消某用户的「强制重新登录」标记，去除 Redis 中的标记后，该用户可再次通过 RememberMe 自动登录。
     */
    @RequestMapping(value = "/clearForceLogout", method = RequestMethod.POST)
    public R clearForceLogout(@RequestBody Map<String, Object> body) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return R.error(403, "无权限");
        }
        Object o = body.get("userUuid");
        if (o == null || StringUtils.isBlank(o.toString())) {
            return R.error(400, "userUuid 不能为空");
        }
        String userUuid = o.toString().trim();
        shiroSessionService.clearForceLogout(userUuid);
        return R.ok().put("msg", "已取消强制重新登录");
    }
}
