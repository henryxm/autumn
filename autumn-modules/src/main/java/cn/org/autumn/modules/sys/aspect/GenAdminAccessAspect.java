package cn.org.autumn.modules.sys.aspect;

import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 管理 API / gen CRUD 统一门禁：系统管理员 + {@code wall_ip_admin_white}（管理 IP）。
 * <p>
 * 织入范围（业务仓标准包亦可命中）：
 * <ul>
 *   <li>{@code *..modules..controller..*}（含非 {@code cn.org.autumn} 前缀）</li>
 *   <li>{@code *..controller.gen..*}</li>
 *   <li>{@code cn.org.autumn.modules.install..*}（向导；判定侧不强制 IP）</li>
 * </ul>
 * 判定与执行见 {@link AdminAccessGuard#enforceAdminIpApiIfRequired}；异形包见 {@code docs/AI_ADMIN_IP_BIZ.md}。
 */
@Slf4j
@Aspect
@Order(0)
@Component
public class GenAdminAccessAspect {

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    @Before("(within(*..modules..controller..*) || within(*..controller.gen..*) || within(cn.org.autumn.modules.install..*)) && execution(public * *(..))")
    public void requireAdminIpForManagementApi(JoinPoint joinPoint) {
        adminAccessGuard.enforceAdminIpApiIfRequired(joinPoint);
    }
}
