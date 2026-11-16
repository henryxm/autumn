package cn.org.autumn.site;

import cn.org.autumn.model.IP;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台访问门禁：
 * <ul>
 *   <li>框架 AdminLTE 壳（{@code index}/{@code index1}/{@code main}）仅系统管理员；</li>
 *   <li>{@code @PageAware(admin = true)} 注册的业务页资源：已登录非系统管理员 → 404。</li>
 * </ul>
 * 业务工程可通过更小 {@code @Order} 的 {@link cn.org.autumn.config.PageHandler} 抢先返回自有模板或 {@code redirect:}；
 * {@code redirect:} 不算壳视图——业务 Handler 抢先 redirect 时<strong>必须</strong>对
 * {@link SysUserRoleService#isSystemAdministrator} 返回空串放行，否则会堵死超管 {@code /admin} 入口。
 */
@Slf4j
@Component
public class AdminShellAccess {

    private static final Set<String> ADMIN_SHELL_VIEWS = Set.of("index", "index1", "main");

    private final Set<String> adminPageResources = ConcurrentHashMap.newKeySet();

    @Autowired
    @Lazy
    SysUserRoleService sysUserRoleService;

    @Autowired
    @Lazy
    PageFactory pageFactory;

    /**
     * 注册仅系统管理员可访问的页面资源路径（与 Site 字段值 / SPM resourceId 一致，可带或不带前导 {@code /}、{@code .html}）。
     */
    public void registerAdminPage(String resourceId) {
        String key = normalizeViewKey(resourceId);
        if (StringUtils.isNotBlank(key)) {
            adminPageResources.add(key);
        }
    }

    public static boolean isAdminShellView(String view) {
        String bare = normalizeViewKey(view);
        if (StringUtils.isBlank(bare) || (view != null && view.trim().startsWith("redirect:"))) {
            return false;
        }
        return ADMIN_SHELL_VIEWS.contains(bare);
    }

    public boolean isAdminPageView(String view) {
        String bare = normalizeViewKey(view);
        return StringUtils.isNotBlank(bare) && adminPageResources.contains(bare);
    }

    public static String normalizeViewKey(String view) {
        if (StringUtils.isBlank(view)) {
            return "";
        }
        String bare = view.trim();
        if (bare.startsWith("redirect:")) {
            return "";
        }
        int q = bare.indexOf('?');
        if (q >= 0) {
            bare = bare.substring(0, q);
        }
        if (bare.startsWith("/")) {
            bare = bare.substring(1);
        }
        if (bare.endsWith(".html")) {
            bare = bare.substring(0, bare.length() - 5);
        }
        return bare;
    }

    /**
     * 后台壳或 {@code @PageAware(admin=true)}：未登录或非系统管理员 → 404；超管原样返回。
     */
    public String denyUnlessSystemAdmin(String view, HttpServletRequest request, HttpServletResponse response, Model model) {
        boolean restricted = isAdminShellView(view) || isAdminPageView(view);
        if (!restricted) {
            return view;
        }
        String key = normalizeViewKey(view);
        if (!ShiroUtils.isLogin()) {
            warnShellDenied("admin_shell_unauthenticated", key, request);
            return pageFactory._404(request, response, model);
        }
        if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return view;
        }
        warnShellDenied("admin_shell_forbidden", key, request);
        return pageFactory._404(request, response, model);
    }

    private void warnShellDenied(String reason, String target, HttpServletRequest request) {
        String user = "-";
        boolean login = false;
        try {
            login = ShiroUtils.isLogin();
            if (login) {
                user = StringUtils.defaultIfBlank(ShiroUtils.getUserUuid(), "-");
            }
        } catch (Exception ignored) {
        }
        String ip = request != null ? StringUtils.defaultIfBlank(IP.getIp(request), "-") : "-";
        String uri = request != null ? StringUtils.defaultIfBlank(request.getRequestURI(), "-") : "-";
        String ua = request != null ? StringUtils.abbreviate(StringUtils.defaultString(request.getHeader("User-Agent")), 120) : "-";
        log.warn("SecurityAlert denied reason={}, user={}, login={}, ip={}, uri={}, target={}, detail=, ua={}", reason, user, login, ip, uri, target, ua);
    }
}
