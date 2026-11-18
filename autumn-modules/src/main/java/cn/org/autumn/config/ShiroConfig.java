package cn.org.autumn.config;

import cn.org.autumn.model.SecuritySessionConfig;
import cn.org.autumn.modules.spm.filter.SpmFilter;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.modules.sys.shiro.ClientIpSessionFactory;
import cn.org.autumn.modules.sys.shiro.ForceLogoutRememberMeManager;
import cn.org.autumn.modules.sys.shiro.HostAwareSessionIdCookie;
import cn.org.autumn.modules.sys.shiro.HostSessionCookieSupport;
import cn.org.autumn.modules.sys.shiro.RedisShiroSessionDAO;
import cn.org.autumn.modules.sys.shiro.RememberMeCipherKeySupport;
import cn.org.autumn.node.ProfileStore;
import cn.org.autumn.modules.sys.shiro.ShiroSessionTimeouts;
import cn.org.autumn.modules.sys.shiro.UserRealm;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.RememberMeManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.servlet.Filter;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@DependsOn({"env"})
public class ShiroConfig {

    /**
     * 读 SysConfig（可能 ENC$）前须完成 {@code fieldEncryptRuntimeService} 的 {@code @PostConstruct} 密钥预热。
     * {@code fieldEncryptRuntimeService} 只挂在这些 Bean 上，勿升到 Configuration 类级（会拖住 LBP 过早建加密 Bean）。
     */
    @Bean("sessionManager")
    @DependsOn("fieldEncryptRuntimeService")
    public SessionManager sessionManager(@Autowired RedisShiroSessionDAO redisShiroSessionDAO,
                                         Environment environment,
                                         @Autowired(required = false) SysConfigService sysConfigService) {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionFactory(new ClientIpSessionFactory());
        // Session 空闲超时 1 天（与 Redis TTL / Cookie maxAge 对齐，见 ShiroSessionTimeouts）
        sessionManager.setGlobalSessionTimeout(ShiroSessionTimeouts.SESSION_TIMEOUT_MS);
        boolean install = InstallMode.isActive(environment);
        // 安装模式：关闭周期校验，避免无意义地解析 SessionId / 触碰 SessionDAO
        sessionManager.setSessionValidationSchedulerEnabled(!install);
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        HostAwareSessionIdCookie cookie = new HostAwareSessionIdCookie(HostSessionCookieSupport.DEFAULT_COOKIE_NAME);
        // Cookie 寿命取记住我上限；真正空闲过期仍由 session.timeout（1 天 / 7 天）决定
        cookie.setMaxAge(ShiroSessionTimeouts.REMEMBER_ME_COOKIE_MAX_AGE_SEC);
        cookie.setHttpOnly(true);
        SecuritySessionConfig sessionCfg = resolveSessionConfig(sysConfigService);
        cookie.setSecure(sessionCfg.isSessionSecure());
        applySameSite(cookie, sessionCfg.getSessionSameSite());
        sessionManager.setSessionIdCookie(cookie);
        // 安装模式禁止自定义 SessionDAO；非安装态始终挂 RedisShiroSessionDAO（无 Redis 时仅 cache+DB）
        // 避免 Bean 存在却未挂载时登录落库、登出不走 doDelete 留下可回源孤儿行
        if (!install) {
            sessionManager.setSessionDAO(redisShiroSessionDAO);
        }
        return sessionManager;
    }

    @Bean
    @DependsOn("fieldEncryptRuntimeService")
    public RememberMeManager rememberMeManager(SimpleCookie rememberMeCookie,
                                               @Autowired(required = false) RedisTemplate redisTemplate,
                                               @Autowired(required = false) StringRedisTemplate stringRedisTemplate,
                                               @Autowired(required = false) ProfileStore profileStore,
                                               Environment environment,
                                               SysConfigService sysConfigService,
                                               @Value("${autumn.shiro.remember-me.key-file:}") String rememberMeKeyFile,
                                               @Value("${autumn.node.namespace:default}") String nodeNamespace) {
        ForceLogoutRememberMeManager manager = new ForceLogoutRememberMeManager();
        // 集群：SysConfig → Redis SETNX → 节点 home/security/remember-me.key（无 Env）
        StringRedisTemplate cipherRedis = InstallMode.isActive(environment) ? null : stringRedisTemplate;
        String ns = sysConfigService != null ? sysConfigService.getNameSpace() : nodeNamespace;
        if (StringUtils.isBlank(ns)) {
            ns = nodeNamespace;
        }
        Path nodeHome = profileStore != null ? profileStore.home() : null;
        Path keyFile = RememberMeCipherKeySupport.resolveKeyFile(rememberMeKeyFile, nodeHome);
        SecuritySessionConfig sessionCfg = resolveSessionConfig(sysConfigService);
        String cipherConfigured = StringUtils.trimToEmpty(sessionCfg.getRememberMeCipherKey());
        manager.setCipherKey(RememberMeCipherKeySupport.resolveCipherKey(
                cipherConfigured, keyFile, cipherRedis, ns));
        manager.setCookie(rememberMeCookie);
        if (InstallMode.isActive(environment)) {
            manager.setRedisTemplate(null);
        } else {
            manager.setRedisTemplate(redisTemplate);
        }
        manager.setSysConfigService(sysConfigService);
        return manager;
    }

    @Bean
    @DependsOn("fieldEncryptRuntimeService")
    public SimpleCookie rememberMeCookie(@Autowired(required = false) SysConfigService sysConfigService) {
        SecuritySessionConfig sessionCfg = resolveSessionConfig(sysConfigService);
        SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
        simpleCookie.setHttpOnly(true);
        simpleCookie.setSecure(sessionCfg.isRememberMeSecure());
        applySameSite(simpleCookie, sessionCfg.getRememberMeSameSite());
        simpleCookie.setMaxAge(7 * 24 * 60 * 60);//7天
        return simpleCookie;
    }

    /**
     * 读 {@link SysConfigService#getSecuritySessionConfig()}：库有行用库；库空则种子（含 prod Secure / 历史 Env）。
     * Cookie/RememberMe Bean 在 Init put 之前创建时依赖此回落，与入库后权威值一致。
     */
    static SecuritySessionConfig resolveSessionConfig(SysConfigService sysConfigService) {
        if (sysConfigService == null) {
            return new SecuritySessionConfig();
        }
        try {
            return sysConfigService.getSecuritySessionConfig();
        } catch (Exception e) {
            return new SecuritySessionConfig();
        }
    }

    /**
     * Shiro {@link SimpleCookie.SameSiteOptions} 枚举名为 NONE/LAX/STRICT（全大写）。
     * 配置写 Lax/lax 时归一化为 LAX，避免 {@code valueOf} 启动失败。
     */
    static void applySameSite(SimpleCookie cookie, String sameSite) {
        if (cookie == null || StringUtils.isBlank(sameSite)) {
            return;
        }
        String normalized = sameSite.trim().toUpperCase();
        cookie.setSameSite(SimpleCookie.SameSiteOptions.valueOf(normalized));
    }

    @Bean("securityManager")
    public SecurityManager securityManager(UserRealm userRealm, SessionManager sessionManager, RememberMeManager rememberMeManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm);
        securityManager.setSessionManager(sessionManager);
        securityManager.setRememberMeManager(rememberMeManager);
        return securityManager;
    }

    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager, List<FilterChainHandler> filterChainHandlers,
                                              @Value("${server.servlet.context-path:}") String servletContextPath) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);
        SecurityUtils.setSecurityManager(securityManager);
//        shiroFilter.setLoginUrl("");//身份认证失败，则跳转到登录页面的配置 没有登录的用户请求需要登录的页面时自动跳转到登录页面，不是必须的属性，不输入地址的话会自动寻找项目web项目的根目录下的”/login.jsp”页面。
//        shiroFilter.setSuccessUrl("");//登录成功默认跳转页面，不配置则跳转至”/”。如果登陆前点击的一个需要登录的页面，则在登录自动跳转到那个需要登录的页面。不跳转到此。
//        shiroFilter.setUnauthorizedUrl("");//没有权限默认跳转的页面
//        shiroFilter.setFilterChainDefinitions("");//filterChainDefinitions的配置顺序为自上而下,以最上面的为准

        //当运行一个Web应用程序时,Shiro将会创建一些有用的默认Filter实例,并自动地在[main]项中将它们置为可用自动地可用的默认的Filter实例是被DefaultFilter枚举类定义的,枚举的名称字段就是可供配置的名称
        /**
         * anon---------------org.apache.shiro.web.filter.authc.AnonymousFilter 没有参数，表示可以匿名使用。
         * authc--------------org.apache.shiro.web.filter.authc.FormAuthenticationFilter 表示需要认证(登录)才能使用，没有参数
         * authcBasic---------org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter 没有参数表示httpBasic认证
         * logout-------------org.apache.shiro.web.filter.authc.LogoutFilter
         * noSessionCreation--org.apache.shiro.web.filter.session.NoSessionCreationFilter
         * perms--------------org.apache.shiro.web.filter.authz.PermissionAuthorizationFilter 参数可以写多个，多个时必须加上引号，并且参数之间用逗号分割，例如/admins/user/**=perms["user:add:*,user:modify:*"]，当有多个参数时必须每个参数都通过才通过，想当于isPermitedAll()方法。
         * port---------------org.apache.shiro.web.filter.authz.PortFilter port[8081],当请求的url的端口不是8081是跳转到schemal://serverName:8081?queryString,其中schmal是协议http或https等，serverName是你访问的host,8081是url配置里port的端口，queryString是你访问的url里的？后面的参数。
         * rest---------------org.apache.shiro.web.filter.authz.HttpMethodPermissionFilter 根据请求的方法，相当于/admins/user/**=perms[user:method] ,其中method为post，get，delete等。
         * roles--------------org.apache.shiro.web.filter.authz.RolesAuthorizationFilter 参数可以写多个，多个时必须加上引号，并且参数之间用逗号分割，当有多个参数时，例如admins/user/**=roles["admin,guest"],每个参数通过才算通过，相当于hasAllRoles()方法。
         * ssl----------------org.apache.shiro.web.filter.authz.SslFilter 没有参数，表示安全的url请求，协议为https
         * user---------------org.apache.shiro.web.filter.authz.UserFilter 没有参数表示必须存在用户，当登入操作时不做检查
         *
         * 通常可将这些过滤器分为两组
         * anon,authc,authcBasic,user是第一组认证过滤器
         * perms,port,rest,roles,ssl是第二组授权过滤器
         * 注意user和authc不同：当应用开启了rememberMe时,用户下次访问时可以是一个user,但绝不会是authc,因为authc是需要重新认证的
         * user表示用户不一定已通过认证,只要曾被Shiro记住过登录状态的用户就可以正常发起请求,比如rememberMe 说白了,以前的一个用户登录时开启了rememberMe,然后他关闭浏览器,下次再访问时他就是一个user,而不会authc
         *
         *
         * 举几个例子
         *  /admin=authc,roles[admin]      表示用户必需已通过认证,并拥有admin角色才可以正常发起'/admin'请求
         *  /edit=authc,perms[admin:edit]  表示用户必需已通过认证,并拥有admin:edit权限才可以正常发起'/edit'请求
         *  /home=user     表示用户不一定需要已经通过认证,只需要曾经被Shiro记住过登录状态就可以正常发起'/home'请求
         *
         * 各默认过滤器常用如下(注意URL Pattern里用到的是两颗星,这样才能实现任意层次的全匹配)
         * /admins/**=anon             无参,表示可匿名使用,可以理解为匿名用户或游客
         *  /admins/user/**=authc       无参,表示需认证才能使用
         *  /admins/user/**=authcBasic  无参,表示httpBasic认证
         *  /admins/user/**=ssl         无参,表示安全的URL请求,协议为https
         *  /admins/user/**=perms[user:add:*]  参数可写多个,多参时必须加上引号,且参数之间用逗号分割,如/admins/user/**=perms["user:add:*,user:modify:*"]。当有多个参数时必须每个参数都通过才算通过,相当于isPermitedAll()方法
         *  /admins/user/**=port[8081] 当请求的URL端口不是8081时,跳转到schemal://serverName:8081?queryString。其中schmal是协议http或https等,serverName是你访问的Host,8081是Port端口,queryString是你访问的URL里的?后面的参数
         *  /admins/user/**=rest[user] 根据请求的方法,相当于/admins/user/**=perms[user:method],其中method为post,get,delete等
         *  /admins/user/**=roles[admin]  参数可写多个,多个时必须加上引号,且参数之间用逗号分割,如：/admins/user/**=roles["admin,guest"]。当有多个参数时必须每个参数都通过才算通过,相当于hasAllRoles()方法
         *
         */
        //Shiro验证URL时,URL匹配成功便不再继续匹配查找(所以要注意配置文件中的URL顺序,尤其在使用通配符时)
        // 配置不会被拦截的链接 顺序判断
        Map<String, String> filterMap = new LinkedHashMap<>();
        Map<String, Filter> map = new HashMap<>();
        map.put("spm", new SpmFilter());
        // 与 ServletContext#getContextPath 对齐：根部署为空串，不依赖 ServletContext Bean（部分测试/切片环境无该 Bean）
        String ctx = StringUtils.defaultString(servletContextPath);
        if ("/".equals(ctx))
            ctx = "";
        String login = ctx + "/login";
        String unauthorized = ctx.isEmpty() ? "/" : (ctx + "/");
        String success = "";

        for (FilterChainHandler filterChainHandler : filterChainHandlers) {
            filterChainHandler.definition(filterMap);
            filterChainHandler.filter(map);
            String l = filterChainHandler.login(login);
            if (StringUtils.isNotBlank(l))
                login = l;
            String u = filterChainHandler.unauthorized(unauthorized);
            if (StringUtils.isNotBlank(u))
                unauthorized = u;
            String s = filterChainHandler.success(success);
            if (StringUtils.isNotBlank(s))
                success = s;
        }
        shiroFilter.setLoginUrl(login);
        shiroFilter.setUnauthorizedUrl(unauthorized);
        if (StringUtils.isNotBlank(success))
            shiroFilter.setSuccessUrl(success);
        if (Config.isDev()) {
            filterMap.put("/swagger/**", "anon");
            filterMap.put("/v2/api-docs", "anon");
            filterMap.put("/swagger-ui.html", "anon");
            filterMap.put("/webjars/**", "anon");
            filterMap.put("/swagger-resources/**", "anon");
            filterMap.put("/druid/**", "anon");
        }

        filterMap.put("/shield/**", "anon");
        filterMap.put("/statics/**", "anon");
        filterMap.put("/static/**", "anon");
        filterMap.put("/files/**", "anon");
        filterMap.put("/js/**", "anon");
        filterMap.put("/css/**", "anon");
        filterMap.put("/images/**", "anon");
        filterMap.put("/api/**", "anon");
        // 机器人开放 API：由 @Authenticated + UserContext 解析 Token / rbt_，不经 Session 表单登录
        filterMap.put("/bot/api/v1/**", "anon");
        filterMap.put("/login.html", "anon");
        filterMap.put("/login", "anon");
        filterMap.put("/auth/login/**", "anon");
        filterMap.put("/register.html", "anon");
        filterMap.put("/register", "anon");
        filterMap.put("/forgotpassword.html", "anon");
        filterMap.put("/forgotpassword", "anon");
        filterMap.put("/user/service.html", "anon");
        filterMap.put("/user/service", "anon");
        filterMap.put("/user/privacy.html", "anon");
        filterMap.put("/user/privacy", "anon");
        filterMap.put("/user/about.html", "anon");
        filterMap.put("/user/about", "anon");
        // 社区行为规范：与协议/隐私一致，匿名可读（H5 + 短链）
        filterMap.put("/user/community.html", "anon");
        filterMap.put("/user/community", "anon");
        filterMap.put("/404.html", "anon");
        filterMap.put("/500.html", "anon");
        filterMap.put("/505.html", "anon");
        filterMap.put("/error.html", "anon");
        filterMap.put("/404", "anon");
        filterMap.put("/500", "anon");
        filterMap.put("/505", "anon");
        filterMap.put("/error", "anon");
        // /plugin、/clear、/reinit：须登录；方法内再校验超管 + IP 白名单 / ops Token
        filterMap.put("/sys/ops/admin-bootstrap/**", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/sys/register", "anon");
        filterMap.put("/sys/forgotpassword", "anon");
        filterMap.put("/sys/account/available", "anon");
        filterMap.put("/sys/using", "anon");
        filterMap.put("/sys/autologin", "anon");
        filterMap.put("/favicon.ico", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/oauth2/**", "anon");
        filterMap.put("/rsa/api/v1/**", "anon");
        filterMap.put("/ops/api/v1/**", "anon");
        filterMap.put("/client/**", "anon");
        // /actuator/**、/plugin/load 不再 anon：需登录（spm）
        filterMap.put("/http/proxy/v1/**", "anon");
        filterMap.put("/install", "anon");
        filterMap.put("/install/**", "anon");
        filterMap.put("/**", "spm");
        shiroFilter.setFilterChainDefinitionMap(filterMap);
        shiroFilter.setFilters(map);
        return shiroFilter;
    }

    /**
     * 必须 static：避免为创建本 BPP 而实例化 {@link ShiroConfig}，连带提前初始化业务 Bean。
     */
    @Bean("lifecycleBeanPostProcessor")
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator proxyCreator = new DefaultAdvisorAutoProxyCreator();
        proxyCreator.setProxyTargetClass(true);
        return proxyCreator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
