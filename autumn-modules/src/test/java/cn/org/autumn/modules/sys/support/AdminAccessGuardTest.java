package cn.org.autumn.modules.sys.support;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.SecurityAuthConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.site.AdminShellAccess;
import cn.org.autumn.site.PageFactory;
import jakarta.websocket.server.ServerEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import cn.org.autumn.config.AdminAccessModuleHandler;
import cn.org.autumn.modules.bot.controller.gen.RobotControllerGen;
import cn.org.autumn.modules.db.controller.DatabaseController;
import cn.org.autumn.modules.gen.controller.GeneratorController;
import cn.org.autumn.modules.install.InstallBootstrapStatusController;
import cn.org.autumn.modules.install.InstallWizardApiController;
import cn.org.autumn.modules.job.controller.ScheduleJobController;
import cn.org.autumn.modules.lan.controller.LanguageApi;
import cn.org.autumn.modules.lan.controller.LanguageController;
import cn.org.autumn.modules.lan.controller.gen.LanguageControllerGen;
import cn.org.autumn.modules.node.controller.ProfileController;
import cn.org.autumn.modules.oauth.controller.ClientDetailsController;
import cn.org.autumn.modules.oauth.controller.OauthAsAdminController;
import cn.org.autumn.modules.oauth.controller.gen.ClientDetailsControllerGen;
import cn.org.autumn.modules.safe.controller.gen.PayUserPinControllerGen;
import cn.org.autumn.modules.sys.controller.AuthLoginProviderController;
import cn.org.autumn.modules.sys.controller.SysLoginController;
import cn.org.autumn.modules.sys.controller.SysMenuController;
import cn.org.autumn.modules.sys.controller.SysUserController;
import cn.org.autumn.modules.sys.controller.SysUsingController;
import cn.org.autumn.modules.usr.controller.gen.UserProfileControllerGen;
import cn.org.autumn.modules.wall.controller.ShieldController;
import cn.org.autumn.modules.wall.controller.WallController;
import cn.org.autumn.modules.wall.controller.gen.IpWhiteControllerGen;
import java.util.List;
import java.util.Set;
import org.mockito.ArgumentMatchers;

class AdminAccessGuardTest {

    private AdminAccessGuard guard;
    private SysUserRoleService sysUserRoleService;
    private SysMenuService sysMenuService;
    private SysConfigService sysConfigService;
    private IpAdminWhiteService ipAdminWhiteService;
    private PageFactory pageFactory;
    private AdminShellAccess adminShellAccess;
    private MockedStatic<ShiroUtils> shiroUtils;

    @BeforeEach
    void setUp() {
        guard = new AdminAccessGuard();
        sysUserRoleService = mock(SysUserRoleService.class);
        sysMenuService = mock(SysMenuService.class);
        sysConfigService = mock(SysConfigService.class);
        ipAdminWhiteService = mock(IpAdminWhiteService.class);
        pageFactory = mock(PageFactory.class);
        adminShellAccess = mock(AdminShellAccess.class);
        ReflectionTestUtils.setField(guard, "sysUserRoleService", sysUserRoleService);
        ReflectionTestUtils.setField(guard, "sysMenuService", sysMenuService);
        ReflectionTestUtils.setField(guard, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(guard, "ipAdminWhiteService", ipAdminWhiteService);
        ReflectionTestUtils.setField(guard, "pageFactory", pageFactory);
        ReflectionTestUtils.setField(guard, "adminShellAccess", adminShellAccess);
        shiroUtils = Mockito.mockStatic(ShiroUtils.class);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(new SecurityAuthConfig());
        when(sysConfigService.isBeanExecuteEnabled()).thenReturn(false);
        when(sysConfigService.isWebShellEnabled()).thenReturn(false);
        when(pageFactory._404(any(), any(), any())).thenReturn("404");
        when(adminShellAccess.isAdminPageView(any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (shiroUtils != null) {
            shiroUtils.close();
        }
    }

    @Test
    void selfView_allowsUsrModule() {
        assertTrue(guard.isSelfView("modules/usr/userprofile"));
        assertTrue(guard.isSelfView("modules/safe/payuserpin"));
        assertFalse(guard.isSelfView("modules/sys/user"));
    }

    @Test
    void genRequiresSystemAdmin_exceptSelfModules() {
        assertTrue(guard.shouldRequireSystemAdminForGen(
                IpWhiteControllerGen.class));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                UserProfileControllerGen.class));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                PayUserPinControllerGen.class));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                RobotControllerGen.class));
        assertTrue(guard.shouldRequireSystemAdminForGen(
                ClientDetailsControllerGen.class));
    }

    @Test
    void adminIpApi_coversGenModulesAndSubclassOverrides() {
        assertTrue(guard.shouldRequireAdminIpApi(
                SysUserController.class, "list", 1));
        assertTrue(guard.shouldRequireAdminIpApi(
                SysUserController.class, "info", 1));
        assertFalse(guard.shouldRequireAdminIpApi(
                SysUserController.class, "info", 0));
        assertFalse(guard.shouldRequireAdminIpApi(
                SysUserController.class, "password", 2));
        assertFalse(guard.shouldRequireAdminIpApi(
                SysMenuController.class, "nav", 1));
        assertTrue(guard.shouldRequireAdminIpApi(
                SysMenuController.class, "list", 0));
        assertTrue(guard.shouldRequireAdminIpApi(
                DatabaseController.class, "list", 1));
        assertTrue(guard.shouldRequireAdminIpApi(
                GeneratorController.class, "code", 3));
        assertTrue(guard.shouldRequireAdminIpApi(
                ScheduleJobController.class, "run", 1));
        assertTrue(guard.shouldRequireAdminIpApi(
                ShieldController.class, "attack", 2));
        assertFalse(guard.shouldRequireAdminIpApi(
                ShieldController.class, "test", 2));
        assertTrue(guard.extendsRestrictedGenController(
                ShieldController.class));
        assertFalse(guard.shouldRequireAdminIpApi(
                WallController.class, "unlockIpAdminWhite", 2));
        assertFalse(guard.shouldRequireAdminIpApi(
                SysLoginController.class, "login", 2));
        assertFalse(guard.shouldRequireAdminIpApi(
                SysUsingController.class, "using", 2));
        assertFalse(guard.shouldRequireAdminIpApi(
                AuthLoginProviderController.class, "providers", 0));
        assertFalse(guard.shouldRequireAdminIpApi(
                UserProfileControllerGen.class, "list", 1));
        assertTrue(guard.shouldRequireAdminIpApi(
                OauthAsAdminController.class, "overview", 1));
        assertTrue(guard.shouldRequireAdminIpApi(
                ProfileController.class, "profile", 0));
        assertTrue(guard.shouldRequireAdminIpApi(
                ProfileController.class, "assign", 1));
        assertTrue(guard.isGenGlobalModule("node"));
        assertFalse(guard.shouldRequireAdminIpApi(
                InstallWizardApiController.class, "types", 0));
        assertFalse(guard.shouldRequireAdminIpApi(
                InstallBootstrapStatusController.class, "status", 0));
    }

    @Test
    void serverEndpointInControllerPackage_exemptFromAdminIpApi() {
        assertFalse(guard.shouldRequireAdminIpApi(SocketEndpointFixture.class, "onOpen", 0));
    }

    @Test
    void methodDeclaringClass_gatesGenCrudNotLanApi() {
        Class<?> oauthCtrl = ClientDetailsController.class;
        Class<?> oauthGen = ClientDetailsControllerGen.class;
        // gen 上 CRUD：仍强制
        assertTrue(guard.shouldRequireAdminIpApi(oauthCtrl, oauthGen, "list", 1));
        // GEN_MODULES 子类运维口（无声明类豁免）仍强制
        assertTrue(guard.shouldRequireAdminIpApi(
                ShieldController.class,
                ShieldController.class,
                "attack", 2));
        assertFalse(guard.shouldRequireAdminIpApi(
                ShieldController.class,
                ShieldController.class,
                "test", 2));
    }

    @Test
    void languageApi_inLanModule_notGatedByModuleBlanket() {
        assertFalse(guard.shouldRequireAdminIpApi(
                LanguageApi.class, "getSupportedLanguage", 1));
        assertTrue(AdminAccessGuard.isPublicApiControllerName(
                LanguageApi.class));
    }

    @Test
    void languageControllerPut_clusterRegister_exemptFromAdminIp() {
        Class<?> ctrl = LanguageController.class;
        Class<?> gen = LanguageControllerGen.class;
        // Feign 集群注册：login 非超管亦不可连坐（生产 system_admin_forbidden）
        assertFalse(guard.shouldRequireAdminIpApi(ctrl, ctrl, "put", 1));
        assertFalse(guard.shouldRequireAdminIpApi(ctrl, "put", 1));
        // 管理面仍强制
        assertTrue(guard.shouldRequireAdminIpApi(ctrl, ctrl, "supportedlist", 1));
        assertTrue(guard.shouldRequireAdminIpApi(ctrl, gen, "list", 1));
    }

    @Test
    void managedModulesSpi_extendsGenGlobalModule() {
        AdminAccessModuleHandler managed = new AdminAccessModuleHandler() {
            @Override
            public Set<String> managedModules() {
                return Set.of("order", "goods");
            }
        };
        ReflectionTestUtils.setField(guard, "adminAccessModuleHandlers", List.of(managed));
        assertTrue(guard.isGenGlobalModule("order"));
        assertTrue(guard.isGenGlobalModule("GOODS"));
        assertFalse(guard.isGenGlobalModule("unknownbiz"));
        ReflectionTestUtils.setField(guard, "adminAccessModuleHandlers", List.of());
        assertFalse(guard.isGenGlobalModule("order"));
    }

    @Test
    void enforceAdminIpApiIfRequired_callsRequireWhenNeeded() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        guard.enforceAdminIpApiIfRequired(
                SysUserController.class, "list", 1);
        verify(ipAdminWhiteService).checkAdmin(any(), eq(AdminAccessGuard.class),
                ArgumentMatchers.contains("SysUserController#list"));
    }

    @Test
    void selfGenModule_spiExtendsExempt() {
        Class<?> oauthGen = ClientDetailsControllerGen.class;
        assertTrue(guard.shouldRequireSystemAdminForGen(oauthGen));
        AdminAccessModuleHandler oauthExempt =
                new AdminAccessModuleHandler() {
                    @Override
                    public Set<String> selfGenModules() {
                        return Set.of("oauth");
                    }
                };
        ReflectionTestUtils.setField(guard, "adminAccessModuleHandlers", List.of(oauthExempt));
        assertFalse(guard.shouldRequireSystemAdminForGen(oauthGen));
        ReflectionTestUtils.setField(guard, "adminAccessModuleHandlers", List.of());
        assertTrue(guard.shouldRequireSystemAdminForGen(oauthGen));
    }

    @Test
    void genGlobalView_requiresAdmin() {
        assertTrue(guard.isGenGlobalView("modules/sys/user"));
        assertTrue(guard.isOpsView("exec"));
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("u1");
        when(sysUserRoleService.isSystemAdministrator("u1")).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        assertEquals("404", guard.gateView("modules/sys/user", request, response, model));
    }

    @Test
    void childProjectModule_allowsNonAdminWhenNotLockdown() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("u1");
        when(sysUserRoleService.isSystemAdministrator("u1")).thenReturn(false);
        assertEquals("modules/bigmodel/pages/index",
                guard.gateView("modules/bigmodel/pages/index", new MockHttpServletRequest(),
                        new MockHttpServletResponse(), new ConcurrentModel()));
    }

    @Test
    void childProjectModule_lockdownRequiresAdmin() {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        cfg.setAdminLockdown(true);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(cfg);
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("u1");
        when(sysUserRoleService.isSystemAdministrator("u1")).thenReturn(false);
        assertEquals("404", guard.gateView("modules/account/index", new MockHttpServletRequest(),
                new MockHttpServletResponse(), new ConcurrentModel()));
    }

    @Test
    void pageAwareAdmin_blocksNonAdminEvenInSelfModule() {
        when(adminShellAccess.isAdminPageView("modules/oauth/clientdetails")).thenReturn(true);
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("u1");
        when(sysUserRoleService.isSystemAdministrator("u1")).thenReturn(false);
        assertEquals("404", guard.gateView("modules/oauth/clientdetails", new MockHttpServletRequest(),
                new MockHttpServletResponse(), new ConcurrentModel()));
    }

    @Test
    void pageAwareAdmin_allowsSystemAdmin() {
        when(adminShellAccess.isAdminPageView("modules/account/pages/loginlimit")).thenReturn(true);
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        assertEquals("modules/account/pages/loginlimit",
                guard.gateView("modules/account/pages/loginlimit", new MockHttpServletRequest(),
                        new MockHttpServletResponse(), new ConcurrentModel()));
    }

    @Test
    void opsView_requiresAdminAndIpWhite() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        assertEquals("exec", guard.gateView("exec", request, response, model));
        verify(ipAdminWhiteService).checkAdmin(eq(request), eq(AdminAccessGuard.class), eq("exec"));
    }

    @Test
    void requireOpsAdmin_rejectsNonAdminWithoutIpCheck() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(AException.class, () -> guard.requireOpsAdmin(request, getClass(), "x"));
        Mockito.verifyNoInteractions(ipAdminWhiteService);
    }

    @Test
    void requireSystemAdmin_rejectsNonAdmin() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("u1");
        when(sysUserRoleService.isSystemAdministrator("u1")).thenReturn(false);
        assertThrows(AException.class, () -> guard.requireSystemAdmin());
    }

    @Test
    void beanExecuteDisabledByDefault() {
        assertFalse(guard.isBeanExecuteEnabled());
    }

    @Test
    void webShellDisabledByDefault() {
        assertFalse(guard.isWebShellEnabled());
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(AException.class, () -> guard.requireWebShellOps(request, getClass(), "ws"));
        Mockito.verifyNoInteractions(ipAdminWhiteService);
    }

    @Test
    void webShellOps_requiresEnabledThenOpsAdmin() {
        when(sysConfigService.isWebShellEnabled()).thenReturn(true);
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        guard.requireWebShellOps(request, getClass(), "ws");
        verify(ipAdminWhiteService).checkAdmin(eq(request), eq(getClass()), eq("ws"));
    }

    @Test
    void opsView_ipWhiteFailureReturns404() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new AException("服务繁忙")).when(ipAdminWhiteService).checkAdmin(any(), any(), any());
        assertEquals("404", guard.gateView("exec", request, new MockHttpServletResponse(), new ConcurrentModel()));
    }

    @Test
    void wall_ipWhiteFailure_allowsLockedPage() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(true);
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new AException("服务繁忙")).when(ipAdminWhiteService).checkAdmin(any(), any(), any());
        ConcurrentModel model = new ConcurrentModel();
        assertEquals("wall", guard.gateView("wall", request, new MockHttpServletResponse(), model));
        assertEquals(Boolean.TRUE, model.getAttribute("wallIpLocked"));
    }

    @Test
    void wall_bootstrapPending_locksWithoutIpCheckPass() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(true);
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ConcurrentModel model = new ConcurrentModel();
        assertEquals("wall", guard.gateView("wall", request, new MockHttpServletResponse(), model));
        assertEquals(Boolean.TRUE, model.getAttribute("wallIpLocked"));
        assertEquals(Boolean.TRUE, model.getAttribute("wallBootstrapPending"));
        Mockito.verify(ipAdminWhiteService, Mockito.never()).checkAdmin(any(), any(), any());
    }

    @Test
    void wall_adminIpDisabled_neverLocks() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        when(ipAdminWhiteService.isAdminIpEnable()).thenReturn(false);
        when(ipAdminWhiteService.isBootstrapPending()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ConcurrentModel model = new ConcurrentModel();
        assertEquals("wall", guard.gateView("wall", request, new MockHttpServletResponse(), model));
        assertEquals(Boolean.FALSE, model.getAttribute("wallIpLocked"));
        assertEquals(Boolean.FALSE, model.getAttribute("wallBootstrapPending"));
        Mockito.verify(ipAdminWhiteService, Mockito.never()).checkAdmin(any(), any(), any());
    }

    @Test
    void requireOpsAdmin_usesAdminIpService() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        guard.requireOpsAdmin(request, getClass(), "x");
        verify(ipAdminWhiteService).checkAdmin(eq(request), eq(getClass()), eq("x"));
    }

    @Test
    void genGlobalView_requiresAdminIp() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals("modules/sys/user",
                guard.gateView("modules/sys/user", request, new MockHttpServletResponse(), new ConcurrentModel()));
        verify(ipAdminWhiteService).checkAdmin(eq(request), eq(AdminAccessGuard.class), eq("modules/sys/user"));
    }

    /** 本测专用：验证 {@code @ServerEndpoint} 豁免，非业务仓夹具。 */
    @ServerEndpoint("/admin-access-guard-fixture/{id}")
    static class SocketEndpointFixture {
        public void onOpen() {
            // no-op
        }
    }

}
