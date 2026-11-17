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
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.site.AdminShellAccess;
import cn.org.autumn.site.PageFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 后台访问统一门禁：运维页（超管 + 管理 IP 白名单）、gen CRUD（默认仅超管+管理 IP，自助模块豁免）、
 * 框架 GEN_MODULES / {@code @PageAware(admin=true)} 管理页、定制管理页（adminLockdown）、自助页放行。
 */
@Slf4j
@Component
public class AdminAccessGuard {

    /** 运维高危页（裸名，无 .html） */
    private static final Set<String> OPS_VIEWS = Set.of(
            "exec", "redis", "cache", "queue", "database", "dbmanage", "tablescan",
            "loopjob", "cluster", "loginlog", "log", "logger", "session", "threading",
            "thread", "functionqueue", "reinit", "fieldencrypt", "configsecure", "shield", "scan",
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
            "sys", "wall", "db", "job", "gen", "oss", "lan", "spm", "node"
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
    private IpAdminWhiteService ipAdminWhiteService;

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
     * WebShell：须功能开启 + 系统管理员 + 管理 IP 白名单。
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
     * 运维 API：已登录系统管理员 + 管理 IP 白名单；未登录/非超管直接 403，不先打 IP 未加白。
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
        ipAdminWhiteService.checkAdmin(request, clazz, method);
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
            return enforceAdminIpView(key, "admin_page_view", request, response, model, view);
        }

        if (isSelfView(key)) {
            return view;
        }

        if (isOpsView(key) || isWallModuleView(key)) {
            if (!isSystemAdmin()) {
                return denyView("ops_view", key, request, response, model);
            }
            // wall：超管可进页；IP 未加白时锁内容，仅显示临时安全密码门禁（勿直接 404）
            if ("wall".equals(stripModulePrefix(key))) {
                return gateWallView(key, request, response, model, view);
            }
            return enforceAdminIpView(key, "ops_ip_white", request, response, model, view);
        }

        if (isGenGlobalView(key)) {
            if (!ShiroUtils.isLogin() || !isSystemAdmin()) {
                return denyView("gen_module_view", key, request, response, model);
            }
            return enforceAdminIpView(key, "gen_module_view", request, response, model, view);
        }

        if (isCustomManageView(key)) {
            if (!ShiroUtils.isLogin()) {
                return denyView("custom_manage_unauthenticated", key, request, response, model);
            }
            if (isAdminLockdown()) {
                if (!isSystemAdmin()) {
                    return denyView("custom_manage_lockdown", key, request, response, model);
                }
                return enforceAdminIpView(key, "custom_manage_lockdown", request, response, model, view);
            }
            if (isSystemAdmin() || hasMenuForView(key)) {
                return enforceAdminIpView(key, "custom_manage", request, response, model, view);
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


    /**
     * gen 全表 API：系统管理员 + 管理 IP 白名单。
     */
    public void requireGenAdmin(String target) {
        requireSystemAdmin(target);
        ipAdminWhiteService.checkAdmin(currentRequest(), getClass(), StringUtils.defaultIfBlank(target, "gen"));
    }

    /**
     * wall.html：已登录超管始终可渲染页面；管理 IP 未加白（或引导中）时标记 {@code wallIpLocked}，
     * 由页面仅展示临时安全密码门禁，验证通过并认领 IP 后再显示内容。
     */
    private String gateWallView(String key, HttpServletRequest request,
                                HttpServletResponse response, Model model, String view) {
        boolean bootstrapPending = ipAdminWhiteService.isBootstrapPending();
        boolean locked = bootstrapPending;
        if (!locked) {
            try {
                ipAdminWhiteService.checkAdmin(request, getClass(), key);
            } catch (AException e) {
                locked = true;
                warnDenied("ops_ip_white_wall_lock", key, request, StringUtils.defaultString(e.getMessage()));
            }
        }
        if (model != null) {
            model.addAttribute("wallIpLocked", locked);
            model.addAttribute("wallBootstrapPending", bootstrapPending);
        }
        return view;
    }

    private String enforceAdminIpView(String key, String reason, HttpServletRequest request,
                                      HttpServletResponse response, Model model, String view) {
        try {
            ipAdminWhiteService.checkAdmin(request, getClass(), key);
            return view;
        } catch (AException e) {
            warnDenied(reason, key, request, StringUtils.defaultString(e.getMessage()));
            return pageFactory._404(request, response, model);
        }
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

    /** 包名中的模块段是否属于框架典型全局表模块，或 SPI {@link AdminAccessModuleHandler#managedModules()}。 */
    public boolean isGenGlobalModule(String module) {
        if (StringUtils.isBlank(module)) {
            return false;
        }
        String m = module.toLowerCase(Locale.ROOT);
        if (GEN_MODULES.contains(m)) {
            return true;
        }
        for (AdminAccessModuleHandler handler : adminAccessModuleHandlers) {
            if (handler == null || handler.managedModules() == null) {
                continue;
            }
            for (String extra : handler.managedModules()) {
                if (m.equalsIgnoreCase(extra)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 供框架切面与业务仓薄切面共用：若 {@link #shouldRequireAdminIpApi} 为 true 则 {@link #requireGenAdmin}。
     * 业务异形包标准接入见 {@code docs/AI_ADMIN_IP_BIZ.md} 模式 C。
     * <p>
     * 切面路径传入方法声明类，避免业务子类扩展的公开/集群 API 被 {@code *ControllerGen} 整类连坐。
     */
    public void enforceAdminIpApiIfRequired(JoinPoint joinPoint) {
        if (joinPoint == null) {
            return;
        }
        Object target = joinPoint.getTarget();
        Class<?> type = target != null ? target.getClass() : joinPoint.getSignature().getDeclaringType();
        Class<?> rawTarget = unwrapProxyClass(type);
        Class<?> declaringType = null;
        if (joinPoint.getSignature() instanceof MethodSignature ms) {
            Method m = ms.getMethod();
            if (m != null) {
                // 取目标类上最具体方法，避免签名落在 *ControllerGen 导致业务公开/集群口被整类连坐
                Method specific = AopUtils.getMostSpecificMethod(m, rawTarget != null ? rawTarget : type);
                declaringType = specific != null ? specific.getDeclaringClass() : m.getDeclaringClass();
            }
        }
        if (declaringType == null) {
            declaringType = joinPoint.getSignature().getDeclaringType();
        }
        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        int argCount = args == null ? 0 : args.length;
        enforceAdminIpApiIfRequired(type, declaringType, method, argCount);
    }

    /**
     * gen 模板显式调用（无声明类）：保守按目标类型判定，与历史 CRUD 门禁一致。
     *
     * @see #enforceAdminIpApiIfRequired(JoinPoint)
     */
    public void enforceAdminIpApiIfRequired(Class<?> type, String methodName, int argCount) {
        enforceAdminIpApiIfRequired(type, null, methodName, argCount);
    }

    public void enforceAdminIpApiIfRequired(Class<?> type, Class<?> declaringType, String methodName, int argCount) {
        if (!shouldRequireAdminIpApi(type, declaringType, methodName, argCount)) {
            return;
        }
        Class<?> raw = unwrapProxyClass(type);
        String targetKey = "api:" + (raw != null ? raw.getName() : "-") + "#" + StringUtils.defaultString(methodName);
        requireGenAdmin(targetKey);
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
        Class<?> raw = unwrapProxyClass(type);
        String name = raw.getName();
        if (!name.contains(".controller.gen.")) {
            return false;
        }
        String module = extractModuleName(raw);
        if (isSelfGenModule(module)) {
            return false;
        }
        return true;
    }

    /**
     * 管理 API 是否强制「系统管理员 + 管理 IP（{@link cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity}）」。
     * <p>
     * 无声明类时保守：非自助 gen / 继承 *ControllerGen / GEN_MODULES 手写管理 Controller / *AdminController。
     * 有声明类时：gen CRUD 与带 {@code @RequiresPermissions} 的管理面仍强制；业务模块子类扩展的公开方法不连坐。
     *
     * @param argCount 方法实参个数（用于区分 {@code SysUserController#info()} 与 {@code info(uuid)}）
     */
    public boolean shouldRequireAdminIpApi(Class<?> type, String methodName, int argCount) {
        return shouldRequireAdminIpApi(type, null, methodName, argCount);
    }

    /**
     * @param declaringType 方法声明类；切面传入；{@code null} 表示保守（gen 模板 / 单测旧调用）
     */
    public boolean shouldRequireAdminIpApi(Class<?> type, Class<?> declaringType, String methodName, int argCount) {
        if (type == null) {
            return false;
        }
        Class<?> raw = unwrapProxyClass(type);
        Class<?> decl = declaringType != null ? unwrapProxyClass(declaringType) : null;
        String method = StringUtils.defaultString(methodName);
        String module = extractModuleName(raw);
        // JSR-356：勿对 @ServerEndpoint 做管理 API 门禁（亦避免薄切面误织入）
        if (raw.isAnnotationPresent(jakarta.websocket.server.ServerEndpoint.class)) {
            return false;
        }
        // 安装向导：仅 autumn.install.mode=true 时装载；无管理 IP 可认领前须可访问
        if ("install".equals(module) || raw.getName().contains(".modules.install.")) {
            return false;
        }
        if (isAdminIpExemptController(raw)) {
            return false;
        }
        if (isAdminIpExemptMethod(raw, method, argCount)) {
            return false;
        }
        // 声明在 gen 基类上的 CRUD：始终强制（非自助）
        if (decl != null && isRestrictedGenDeclaringType(decl)) {
            return true;
        }
        if (extendsRestrictedGenController(raw)) {
            if (decl != null && !isRestrictedGenDeclaringType(decl)) {
                // 业务子类扩展方法：有权限注解仍强制；框架 GEN_MODULES 子类运维口仍强制；其余放行
                if (methodHasRequiresPermissions(decl, method, argCount)) {
                    return true;
                }
                return isGenGlobalModule(module);
            }
            // 无声明类（保守）或声明即 gen：整类仍强制
            return true;
        }
        if (isGenGlobalModule(module)) {
            // LanguageApi 等公开面：勿因模块毯误拦
            return !isPublicApiControllerName(raw);
        }
        String simple = raw.getSimpleName();
        return simple.endsWith("AdminController");
    }

    public boolean shouldRequireAdminIpApi(Class<?> type, String methodName) {
        return shouldRequireAdminIpApi(type, methodName, -1);
    }

    /** 方法声明类是否为受限 gen（{@code *.controller.gen.*} 或 {@code *ControllerGen}，非自助）。 */
    public boolean isRestrictedGenDeclaringType(Class<?> declaringType) {
        if (declaringType == null) {
            return false;
        }
        Class<?> decl = unwrapProxyClass(declaringType);
        if (shouldRequireSystemAdminForGen(decl)) {
            return true;
        }
        return decl.getSimpleName().endsWith("ControllerGen") && !isSelfGenModule(extractModuleName(decl));
    }

    /** 公开 API 控制器命名：{@code *Api} / {@code *ApiController}。 */
    public static boolean isPublicApiControllerName(Class<?> type) {
        if (type == null) {
            return false;
        }
        String simple = unwrapProxyClass(type).getSimpleName();
        return simple.endsWith("ApiController") || simple.endsWith("Api");
    }

    private boolean methodHasRequiresPermissions(Class<?> declaringType, String methodName, int argCount) {
        if (declaringType == null || StringUtils.isBlank(methodName)) {
            return false;
        }
        Method matched = findMethodByNameAndArgCount(declaringType, methodName, argCount);
        if (matched == null) {
            return false;
        }
        return AnnotationUtils.findAnnotation(matched, RequiresPermissions.class) != null;
    }

    private static Method findMethodByNameAndArgCount(Class<?> type, String methodName, int argCount) {
        Class<?> c = type;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) {
                    continue;
                }
                if (argCount < 0 || m.getParameterCount() == argCount) {
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /** 继承受限 gen 基类（方法声明在父包时仍强制）。 */
    public boolean extendsRestrictedGenController(Class<?> type) {
        for (Class<?> c = unwrapProxyClass(type); c != null && c != Object.class; c = c.getSuperclass()) {
            if (shouldRequireSystemAdminForGen(c)) {
                return true;
            }
            if (c.getSimpleName().endsWith("ControllerGen") && !isSelfGenModule(extractModuleName(c))) {
                return true;
            }
        }
        return false;
    }

    public static Class<?> unwrapProxyClass(Class<?> type) {
        if (type == null) {
            return null;
        }
        String name = type.getName();
        if (name.contains("$$")) {
            Class<?> s = type.getSuperclass();
            if (s != null && s != Object.class) {
                return s;
            }
        }
        return type;
    }

    private boolean isAdminIpExemptController(Class<?> type) {
        String name = type.getName();
        String simple = type.getSimpleName();
        // 登录/页面路由/公开探测；Wall 自带 requireOps + unlock 例外
        if ("SysLoginController".equals(simple)
                || "SysPageController".equals(simple)
                || "SysProxyController".equals(simple)
                || "SysUsingController".equals(simple)
                || "AuthLoginProviderController".equals(simple)
                || "AdminBootstrapController".equals(simple)
                || "WallController".equals(simple)
                || "SpmController".equals(simple)) {
            return true;
        }
        // oauth2 / 开放 API 等非后台壳
        if (name.contains(".oauth.oauth2.")
                || name.contains(".opl.oauth2.")
                || name.contains(".bot.controller.RobotApi")
                || name.contains(".bot.controller.RobotInbound")
                || name.contains(".qrc.controller.QrcApi")
                || name.contains(".opl.controller.OplApi")
                || name.contains(".opc.controller.OpcApi")) {
            return true;
        }
        return false;
    }

    private boolean isAdminIpExemptMethod(Class<?> type, String method, int argCount) {
        if (StringUtils.isBlank(method)) {
            return false;
        }
        String simple = type.getSimpleName();
        // 任意用户自助会话
        if (method.startsWith("self")
                || "snoozePasswordReminder".equals(method)
                || "acknowledgePasswordReminder".equals(method)) {
            return true;
        }
        // 登录用户导航 / 本人资料与改密
        if ("SysMenuController".equals(simple) && "nav".equals(method)) {
            return true;
        }
        if ("SysUserController".equals(simple)) {
            if ("password".equals(method)) {
                return true;
            }
            // 仅无业务参的 info()（本人）；info(uuid) 仍强制管理 IP
            if ("info".equals(method) && argCount == 0) {
                return true;
            }
        }
        // 防火墙探测入口（故意公开）
        if ("ShieldController".equals(simple) && "test".equals(method)) {
            return true;
        }
        // 集群/子系统 Feign 注册多语言资源（非后台 CRUD；见 LanguageController 注释）
        if ("LanguageController".equals(simple) && "put".equals(method)) {
            return true;
        }
        return false;
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
