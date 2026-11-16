package cn.org.autumn.modules.sys.support;

import cn.org.autumn.config.AdminAccessModuleHandler;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.IP;
import cn.org.autumn.model.SecurityAuthConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.site.AdminShellAccess;
import cn.org.autumn.site.PageFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 后台访问统一门禁：运维页（超管 + IP 白名单）、gen CRUD（默认仅超管，自助模块豁免）、
 * 框架 GEN_MODULES / {@code @PageAware(admin=true)} 管理页、定制管理页（adminLockdown）、自助页放行。
 */
@Slf4j
@Component
public class AdminAccessGuard {

    /** 运维高危页（裸名，无 .html） */
    private static final Set<String> OPS_VIEWS = Set.of(
            "exec", "redis", "cache", "queue", "database", "dbmanage", "tablescan",
            "loopjob", "cluster", "loginlog", "log", "logger", "session", "threading",
            "thread", "functionqueue", "reinit", "fieldencrypt", "shield", "scan",
            "wall", "siteportal", "firewall", "plugin", "classpath", "clear"
    );

    /** 自助 / 公开业务模块前缀（modules/{module}/...） */
    private static final Set<String> SELF_MODULES = Set.of(
            "usr", "safe", "docs", "bot", "qrc", "oauth", "opc", "opl", "client", "auth"
    );

    /**
     * 框架内「典型全局表」模块（页面门禁仍仅超管）。
     * gen API 侧见 {@link #shouldRequireSystemAdminForGen(Class)}：默认拦截全部 gen，仅自助模块豁免。
     */
    public static final Set<String> GEN_MODULES = Set.of(
            "sys", "wall", "db", "job", "gen", "oss", "lan", "spm"
    );

    /**
     * gen CRUD 豁免模块（普通用户本人数据）：不强制系统管理员。
     * 子项目可通过 {@link AdminAccessModuleHandler#selfGenModules()} 追加。
     */
    public static final Set<String> SELF_GEN_MODULES = Set.of(
            "usr", "safe", "bot", "qrc"
    );

    /** 非 gen 定制管理页（裸名） */
    private static final Set<String> CUSTOM_MANAGE_VIEWS = Set.of(
            "oauthasmanage", "authscopemanage", "oauthrpmanage",
            "oplmanage", "opcmanage", "connectbind"
    );

    /** 自助页中仍属管理控制台的例外（需超管） */
    private static final Set<String> SELF_MODULE_ADMIN_SUFFIXES = Set.of(
            "oauthasmanage", "authscopemanage", "oauthrpmanage",
            "oplmanage", "opcmanage", "connectbind",
            "clientdetails", "tokenstore", "encryptkey",
            "openapp", "openaccount", "openidentity", "openunion", "opencode", "opentoken",
            "connectapp", "weboauthbind",
            "scopedefinition", "securityrequest"
    );

    @Autowired
    @Lazy
    private SysUserRoleService sysUserRoleService;

    @Autowired
    @Lazy
    private SysMenuService sysMenuService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @Autowired
    @Lazy
    private PageFactory pageFactory;

    @Autowired
    @Lazy
    private AdminShellAccess adminShellAccess;

    @Autowired(required = false)
    private List<AdminAccessModuleHandler> adminAccessModuleHandlers = Collections.emptyList();

    public boolean isSystemAdmin() {
        return ShiroUtils.isLogin() && sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
    }

    public boolean isAdminLockdown() {
        SecurityAuthConfig cfg = sysConfigService.getSecurityAuthConfig();
        return cfg != null && cfg.isAdminLockdown();
    }

    public boolean isBeanExecuteEnabled() {
        SecurityOpsConfig cfg = sysConfigService.getSecurityOpsConfig();
        return cfg != null && cfg.isBeanExecuteEnabled();
    }

    /** WebShell 总开关（默认关）；与业务属性 autumn.webshell.enabled 取更严。 */
    public boolean isWebShellEnabled() {
        SecurityOpsConfig cfg = sysConfigService.getSecurityOpsConfig();
        return cfg != null && cfg.isWebShellEnabled();
    }

    /**
     * WebShell：须功能开启 + 系统管理员 + IP 白名单。
     */
    public void requireWebShellOps(HttpServletRequest request, Class<?> clazz, String method) {
        String target = (clazz != null ? clazz.getSimpleName() : "-") + "#" + StringUtils.defaultIfBlank(method, "-");
        if (!isWebShellEnabled()) {
            warnDenied("webshell_disabled", target, request, "");
            throw new AException("无权限", 403);
        }
        requireOpsAdmin(request, clazz, method);
        String uuid = "-";
        try {
            if (ShiroUtils.isLogin()) {
                uuid = StringUtils.defaultIfBlank(ShiroUtils.getUserUuid(), "-");
            }
        } catch (Exception ignored) {
        }
        auditWithIp(request, "webshell_access", "userUuid=" + uuid + " target=" + target);
    }

    /**
     * 运维 API：已登录系统管理员 + IP 白名单；未登录/非超管直接 403，不先打 IP 未加白。
     */
    public void requireOpsAdmin(HttpServletRequest request, Class<?> clazz, String method) {
        String target = (clazz != null ? clazz.getSimpleName() : "-") + "#" + StringUtils.defaultIfBlank(method, "-");
        if (!ShiroUtils.isLogin()) {
            warnDenied("ops_admin_unauthenticated", target, request, "");
            throw new AException("无权限", 403);
        }
        if (!sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            warnDenied("ops_admin_forbidden", target, request, "");
            throw new AException("无权限", 403);
        }
        ipWhiteService.check(request, clazz, method);
    }

    /** 仅系统管理员（无 IP 白名单），用于 gen 全表 API。 */
    public void requireSystemAdmin() {
        requireSystemAdmin("system_admin");
    }

    /** 仅系统管理员；{@code target} 写入告警日志便于定位探测路径。 */
    public void requireSystemAdmin(String target) {
        HttpServletRequest request = currentRequest();
        String t = StringUtils.defaultIfBlank(target, "system_admin");
        if (!ShiroUtils.isLogin()) {
            warnDenied("system_admin_unauthenticated", t, request, "");
            throw new AException("无权限", 403);
        }
        if (!sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            warnDenied("system_admin_forbidden", t, request, "");
            throw new AException("无权限", 403);
        }
    }

    /**
     * 页面出口门禁：不允许则返回 404 视图名。
     */
    public String gateView(String view, HttpServletRequest request, HttpServletResponse response, Model model) {
        if (StringUtils.isBlank(view) || view.trim().startsWith("redirect:")) {
            return view;
        }
        String key = AdminShellAccess.normalizeViewKey(view);
        if (StringUtils.isBlank(key)) {
            return view;
        }

        // @PageAware(admin=true) 与后台壳：优先于自助判定，避免误放行
        if (AdminShellAccess.isAdminShellView(view)
                || (adminShellAccess != null && adminShellAccess.isAdminPageView(view))) {
            if (!ShiroUtils.isLogin() || !isSystemAdmin()) {
                return denyView("admin_page_view", key, request, response, model);
            }
            return view;
        }

        if (isSelfView(key)) {
            return view;
        }

        if (isOpsView(key) || isWallModuleView(key)) {
            if (!isSystemAdmin()) {
                return denyView("ops_view", key, request, response, model);
            }
            try {
                ipWhiteService.check(request, getClass(), key);
            } catch (AException e) {
                warnDenied("ops_ip_white", key, request, StringUtils.defaultString(e.getMessage()));
                return pageFactory._404(request, response, model);
            }
            return view;
        }

        if (isGenGlobalView(key)) {
            if (!ShiroUtils.isLogin() || !isSystemAdmin()) {
                return denyView("gen_module_view", key, request, response, model);
            }
            return view;
        }

        if (isCustomManageView(key)) {
            if (!ShiroUtils.isLogin()) {
                return denyView("custom_manage_unauthenticated", key, request, response, model);
            }
            if (isAdminLockdown()) {
                if (!isSystemAdmin()) {
                    return denyView("custom_manage_lockdown", key, request, response, model);
                }
                return view;
            }
            if (isSystemAdmin() || hasMenuForView(key)) {
                return view;
            }
            return denyView("custom_manage_no_menu", key, request, response, model);
        }

        // 子项目自定义 modules/**、pages/**：兼容默认放行（登录由 SpmFilter 等处理）
        // adminLockdown=true 时收成仅超管
        if (key.startsWith("modules/") || key.startsWith("pages/")) {
            if (isAdminLockdown() && !isSystemAdmin()) {
                return denyView("admin_lockdown", key, request, response, model);
            }
            return view;
        }

        return view;
    }

    private String denyView(String reason, String target, HttpServletRequest request,
                            HttpServletResponse response, Model model) {
        warnDenied(reason, target, request, "");
        return pageFactory._404(request, response, model);
    }

    /**
     * 越权 / 未授权探测告警。检索关键字：{@code SecurityAlert denied}。
     * reason 取值见 docs/AI_SECURITY_HARDENING_3_0_COMPAT.md §4.4.2。
     */
    public void warnDenied(String reason, String target, HttpServletRequest request, String detail) {
        boolean login = false;
        String user = "-";
        try {
            login = ShiroUtils.isLogin();
            if (login) {
                user = StringUtils.defaultIfBlank(ShiroUtils.getUserUuid(), "-");
            }
        } catch (Exception ignored) {
        }
        HttpServletRequest req = request != null ? request : currentRequest();
        String ip = req != null ? StringUtils.defaultIfBlank(IP.getIp(req), "-") : "-";
        String uri = "-";
        String ua = "-";
        if (req != null) {
            uri = StringUtils.defaultIfBlank(req.getRequestURI(), "-");
            ua = StringUtils.abbreviate(StringUtils.defaultString(req.getHeader("User-Agent")), 120);
        }
        String r = StringUtils.defaultIfBlank(reason, "unknown");
        String t = StringUtils.defaultIfBlank(target, "-");
        String d = StringUtils.defaultString(detail);
        log.warn("SecurityAlert denied reason={}, user={}, login={}, ip={}, uri={}, target={}, detail={}, ua={}", r, user, login, ip, uri, t, d, ua);
    }

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 包名中的模块段是否属于框架典型全局表模块（sys/wall/db/...）。 */
    public boolean isGenGlobalModule(String module) {
        return StringUtils.isNotBlank(module) && GEN_MODULES.contains(module.toLowerCase(Locale.ROOT));
    }

    /** 是否属于 gen 自助豁免模块（含 SPI）。 */
    public boolean isSelfGenModule(String module) {
        if (StringUtils.isBlank(module)) {
            return false;
        }
        String m = module.toLowerCase(Locale.ROOT);
        if (SELF_GEN_MODULES.contains(m)) {
            return true;
        }
        for (AdminAccessModuleHandler handler : adminAccessModuleHandlers) {
            if (handler == null || handler.selfGenModules() == null) {
                continue;
            }
            for (String extra : handler.selfGenModules()) {
                if (m.equalsIgnoreCase(extra)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从类型名解析 {@code ...modules.{module}...} 中的模块短名；无法解析则空串。
     */
    public static String extractModuleName(Class<?> type) {
        if (type == null) {
            return "";
        }
        String name = type.getName();
        String marker = ".modules.";
        int i = name.indexOf(marker);
        if (i < 0) {
            return "";
        }
        String rest = name.substring(i + marker.length());
        int dot = rest.indexOf('.');
        if (dot <= 0) {
            return "";
        }
        return rest.substring(0, dot);
    }

    /**
     * gen Controller 是否强制系统管理员。
     * <p>
     * 规则：位于 {@code *.controller.gen.*} 且模块<strong>不</strong>在自助豁免列表 → 强制超管。
     * 覆盖 im/vshort/xsignw/bighub 等业务 gen，防止有菜单的非超管读全表。
     */
    public boolean shouldRequireSystemAdminForGen(Class<?> type) {
        if (type == null) {
            return false;
        }
        String name = type.getName();
        if (!name.contains(".controller.gen.")) {
            return false;
        }
        String module = extractModuleName(type);
        if (isSelfGenModule(module)) {
            return false;
        }
        return true;
    }

    /** @deprecated 使用 {@link #shouldRequireSystemAdminForGen(Class)} */
    public boolean isGenGlobalController(Class<?> type) {
        return shouldRequireSystemAdminForGen(type);
    }

    public boolean isOpsView(String viewKey) {
        String bare = stripModulePrefix(viewKey);
        return OPS_VIEWS.contains(bare);
    }

    public boolean isSelfView(String viewKey) {
        if (StringUtils.isBlank(viewKey)) {
            return false;
        }
        String key = viewKey.toLowerCase(Locale.ROOT);
        if (key.startsWith("modules/")) {
            String rest = key.substring("modules/".length());
            int slash = rest.indexOf('/');
            String module = slash < 0 ? rest : rest.substring(0, slash);
            if (!SELF_MODULES.contains(module)) {
                return false;
            }
            String leaf = slash < 0 ? "" : rest.substring(slash + 1);
            String leafBare = leaf;
            if (leafBare.endsWith(".html")) {
                leafBare = leafBare.substring(0, leafBare.length() - 5);
            }
            if (leafBare.endsWith(".js")) {
                leafBare = leafBare.substring(0, leafBare.length() - 3);
            }
            // oauth/opc 等模块内管理页不算自助
            if (SELF_MODULE_ADMIN_SUFFIXES.contains(leafBare) || CUSTOM_MANAGE_VIEWS.contains(leafBare)) {
                return false;
            }
            if (leaf.contains("/pages/") || leaf.startsWith("pages/")) {
                // pages 下默认业务页；管理例外已在 suffix 集合
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean isGenGlobalView(String viewKey) {
        if (StringUtils.isBlank(viewKey) || !viewKey.startsWith("modules/")) {
            return false;
        }
        String rest = viewKey.substring("modules/".length());
        int slash = rest.indexOf('/');
        String module = slash < 0 ? rest : rest.substring(0, slash);
        return GEN_MODULES.contains(module);
    }

    public boolean isWallModuleView(String viewKey) {
        return StringUtils.isNotBlank(viewKey) && viewKey.startsWith("modules/wall/");
    }

    public boolean isCustomManageView(String viewKey) {
        String bare = stripModulePrefix(viewKey);
        if (CUSTOM_MANAGE_VIEWS.contains(bare)) {
            return true;
        }
        // modules/xxx/yyy 叶子命中定制管理名
        if (viewKey != null && viewKey.startsWith("modules/")) {
            int last = viewKey.lastIndexOf('/');
            if (last >= 0) {
                String leaf = viewKey.substring(last + 1);
                return CUSTOM_MANAGE_VIEWS.contains(leaf) || SELF_MODULE_ADMIN_SUFFIXES.contains(leaf);
            }
        }
        return false;
    }

    private boolean hasMenuForView(String viewKey) {
        if (!ShiroUtils.isLogin()) {
            return false;
        }
        String uuid = ShiroUtils.getUserUuid();
        List<SysMenuEntity> menus = sysMenuService.getUserMenuList(uuid);
        if (menus == null || menus.isEmpty()) {
            return false;
        }
        String needle = viewKey;
        String needleHtml = viewKey.endsWith(".html") ? viewKey : viewKey + ".html";
        return menuTreeContains(menus, needle, needleHtml);
    }

    private boolean menuTreeContains(List<SysMenuEntity> menus, String needle, String needleHtml) {
        for (SysMenuEntity menu : menus) {
            String url = menu.getUrl();
            if (StringUtils.isNotBlank(url)) {
                String u = url.startsWith("/") ? url.substring(1) : url;
                if (u.equals(needle) || u.equals(needleHtml)
                        || AdminShellAccess.normalizeViewKey(u).equals(needle)) {
                    return true;
                }
            }
            if (menu.getList() != null && !menu.getList().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<SysMenuEntity> children = (List<SysMenuEntity>) menu.getList();
                if (menuTreeContains(children, needle, needleHtml)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String stripModulePrefix(String viewKey) {
        if (StringUtils.isBlank(viewKey)) {
            return "";
        }
        String key = viewKey;
        int last = key.lastIndexOf('/');
        if (last >= 0) {
            key = key.substring(last + 1);
        }
        return key;
    }

    public void audit(String action, String detail) {
        String uuid = "-";
        try {
            if (ShiroUtils.isLogin()) {
                uuid = StringUtils.defaultIfBlank(ShiroUtils.getUserUuid(), "-");
            }
        } catch (Exception ignored) {
        }
        log.info("SecurityAlert audit action={}, user={}, detail={}", action, uuid, detail);
    }

    public void auditWithIp(HttpServletRequest request, String action, String detail) {
        String ip = request != null ? IP.getIp(request) : "-";
        String uuid = "-";
        try {
            if (ShiroUtils.isLogin()) {
                uuid = StringUtils.defaultIfBlank(ShiroUtils.getUserUuid(), "-");
            }
        } catch (Exception ignored) {
        }
        log.info("SecurityAlert audit action={}, user={}, ip={}, detail={}", action, uuid, ip, detail);
    }
}
