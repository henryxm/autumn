package cn.org.autumn.site;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * Autumn 默认 AdminLTE 后台壳（{@code index}/{@code index1}/{@code main}）仅系统管理员可访问。
 * 业务工程可通过更小 {@code @Order} 的 {@link cn.org.autumn.config.PageHandler} 抢先返回自有模板或 {@code redirect:}；
 * {@code redirect:} 不算壳视图，门禁不会介入——因此业务 Handler 抢先 redirect 时<strong>必须</strong>对
 * {@link SysUserRoleService#isSystemAdministrator} 返回空串放行，否则会堵死超管 {@code /admin} 入口。
 * 仅当最终视图名为上述壳名时才对非管理员返回 404。
 */
@Component
public class AdminShellAccess {

    private static final Set<String> ADMIN_SHELL_VIEWS = Set.of("index", "index1", "main");

    @Autowired
    @Lazy
    SysUserRoleService sysUserRoleService;

    @Autowired
    @Lazy
    PageFactory pageFactory;

    public static boolean isAdminShellView(String view) {
        if (StringUtils.isBlank(view))
            return false;
        String bare = view.trim();
        if (bare.startsWith("redirect:"))
            return false;
        int q = bare.indexOf('?');
        if (q >= 0)
            bare = bare.substring(0, q);
        if (bare.startsWith("/"))
            bare = bare.substring(1);
        if (bare.endsWith(".html"))
            bare = bare.substring(0, bare.length() - 5);
        return ADMIN_SHELL_VIEWS.contains(bare);
    }

    /**
     * 已登录且非系统管理员命中框架后台壳时改为 404；其它情况原样返回。
     */
    public String denyUnlessSystemAdmin(String view, HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!isAdminShellView(view))
            return view;
        if (!ShiroUtils.isLogin())
            return view;
        if (sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid()))
            return view;
        return pageFactory._404(request, response, model);
    }
}
