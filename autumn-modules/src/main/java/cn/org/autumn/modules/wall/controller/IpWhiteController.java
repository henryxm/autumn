package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.controller.gen.IpWhiteControllerGen;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP白名单（非 gen 层加固：写操作须超管 + IP 白名单）
 */
@RestController
@RequestMapping("wall/ipwhite")
public class IpWhiteController extends IpWhiteControllerGen {

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    @Override
    public R save(@RequestBody IpWhiteEntity ipWhite) {
        adminAccessGuard.requireOpsAdmin(currentRequest(), getClass(), "save");
        return super.save(ipWhite);
    }

    @Override
    public R update(@RequestBody IpWhiteEntity ipWhite) {
        adminAccessGuard.requireOpsAdmin(currentRequest(), getClass(), "update");
        return super.update(ipWhite);
    }

    @Override
    public R delete(@RequestBody Long[] ids) {
        adminAccessGuard.requireOpsAdmin(currentRequest(), getClass(), "delete");
        return super.delete(ids);
    }
}
