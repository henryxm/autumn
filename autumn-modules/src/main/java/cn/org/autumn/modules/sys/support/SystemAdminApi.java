package cn.org.autumn.modules.sys.support;

import cn.org.autumn.exception.AException;
import cn.org.autumn.utils.R;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 系统管理员 REST API 统一鉴权与异常包装（超管 + 管理 IP 白名单）。 */
@Slf4j
@Component
public class SystemAdminApi {

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    /**
     * 管理 API：系统管理员 + 管理 IP 白名单（与 {@link #executeOps} 同门禁）。
     * Cookie 会话写操作另受 CSRF（{@code SECURITY_AUTH_CONFIG.csrfEnforce}）约束。
     */
    public R execute(HttpServletRequest request, String logTag, Supplier<R> action) {
        return executeOps(request, logTag, action);
    }

    /** 运维级：系统管理员 + IP 白名单。 */
    public R executeOps(HttpServletRequest request, String logTag, Supplier<R> action) {
        try {
            adminAccessGuard.requireOpsAdmin(request, getClass(), logTag);
            return action.get();
        } catch (AException e) {
            int code = e.getCode() > 0 ? e.getCode() : 403;
            return R.error(code, e.getMessage());
        } catch (Exception e) {
            log.error("{} ops admin API failed: {}", logTag, e.getMessage(), e);
            return R.error(e.getMessage());
        }
    }
}
