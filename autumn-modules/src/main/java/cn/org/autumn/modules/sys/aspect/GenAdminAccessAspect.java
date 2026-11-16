package cn.org.autumn.modules.sys.aspect;

import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 生成 CRUD（{@code *.controller.gen.*}）默认仅系统管理员 + 管理 IP 白名单，防止菜单权限用户越权读全表。
 * <p>
 * 豁免：框架自助模块 usr/safe/bot/qrc，以及子项目通过
 * {@link cn.org.autumn.config.AdminAccessModuleHandler#selfGenModules()} 注册的模块。
 * 拒绝时打 {@code SecurityAlert denied reason=system_admin_*}（见 {@link AdminAccessGuard#warnDenied}）。
 */
@Slf4j
@Aspect
@Component
public class GenAdminAccessAspect {

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    @Before("execution(* *..controller.gen..*(..))")
    public void requireSystemAdminForGlobalGen(JoinPoint joinPoint) {
        Class<?> type = joinPoint.getSignature().getDeclaringType();
        if (adminAccessGuard.shouldRequireSystemAdminForGen(type)) {
            String method = joinPoint.getSignature().getName();
            String target = "gen:" + type.getName() + "#" + method;
            adminAccessGuard.requireGenAdmin(target);
        }
    }
}
