package cn.org.autumn.modules.usr.controller;

import cn.org.autumn.modules.usr.controller.gen.UserLoginLogControllerGen;
import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;
import cn.org.autumn.utils.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 登录日志
 *
 * @author User
 * @email henryxm@163.com
 * @date 2025-12
 */
@RestController
@RequestMapping("usr/userloginlog")
public class UserLoginLogController extends UserLoginLogControllerGen {

    /**
     * 清理：按指定 IP、UUID、时间范围、当前筛选条件或指定 ID 列表删除。
     * <p>
     * 请求体：{ ip?, uuid?, account?, host?, way?, allow?, logout?, createStart?, createEnd?, ids? }
     * - ids 非空时：仅删除这些 ID，忽略其它条件
     * - 否则：按其它条件删除，IP、UUID、登录账号、主机、开始日期、结束日期至少填一项
     */
    @RequestMapping(value = "/clean", method = RequestMethod.POST)
    @RequiresPermissions("usr:userloginlog:delete")
    public R clean(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("ids");
        if (idsObj instanceof Collection && !((Collection<?>) idsObj).isEmpty()) {
            List<Long> idList = new ArrayList<>();
            for (Object o : (Collection<?>) idsObj) {
                if (o instanceof Number) {
                    idList.add(((Number) o).longValue());
                }
            }
            if (!idList.isEmpty()) {
                userLoginLogService.deleteBatchIds(idList);
                return R.ok().put("deleted", idList.size()).put("msg", "已清理 " + idList.size() + " 条");
            }
        }
        String ip = trim(body.get("ip"));
        String uuid = trim(body.get("uuid"));
        String account = trim(body.get("account"));
        String host = trim(body.get("host"));
        String session = trim(body.get("session"));
        String path = trim(body.get("path"));
        String createStart = trim(body.get("createStart"));
        String createEnd = trim(body.get("createEnd"));
        String limitStr = trim(body.get("limit"));
        if (ip.isEmpty() && uuid.isEmpty() && account.isEmpty() && host.isEmpty() && session.isEmpty() && path.isEmpty() && createStart.isEmpty() && createEnd.isEmpty() && limitStr.isEmpty()) {
            return R.error(400, "请指定清理条件：IP、UUID、登录账号、主机、会话、路径、开始日期、结束日期、限制至少填一项");
        }
        int n = userLoginLogService.deleteByParams(body);
        return R.ok().put("deleted", n).put("msg", "已清理 " + n + " 条");
    }

    /**
     * 仅更新白名单：开启或关闭该记录对应的 IP 白名单。
     * 请求体：{ id: Long, white: boolean }
     */
    @RequestMapping(value = "/updateWhite", method = RequestMethod.POST)
    @RequiresPermissions("usr:userloginlog:update")
    public R updateWhite(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        Object whiteObj = body.get("white");
        if (idObj == null) return R.error(400, "缺少 id");
        Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
        boolean white = whiteObj != null && ("true".equalsIgnoreCase(String.valueOf(whiteObj)) || Boolean.TRUE.equals(whiteObj) || "1".equals(String.valueOf(whiteObj)));
        UserLoginLogEntity e = userLoginLogService.selectById(id);
        if (e == null) return R.error(404, "记录不存在");
        e.setWhite(white);
        userLoginLogService.updateById(e);
        return R.ok();
    }

    private static String trim(Object o) {
        return o != null ? o.toString().trim() : "";
    }
}
