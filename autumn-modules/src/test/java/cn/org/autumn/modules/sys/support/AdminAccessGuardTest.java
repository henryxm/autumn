package cn.org.autumn.modules.sys.support;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.SecurityAuthConfig;
import cn.org.autumn.model.SecurityOpsConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.site.AdminShellAccess;
import cn.org.autumn.site.PageFactory;
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

class AdminAccessGuardTest {

    private AdminAccessGuard guard;
    private SysUserRoleService sysUserRoleService;
    private SysMenuService sysMenuService;
    private SysConfigService sysConfigService;
    private IpWhiteService ipWhiteService;
    private PageFactory pageFactory;
    private AdminShellAccess adminShellAccess;
    private MockedStatic<ShiroUtils> shiroUtils;

    @BeforeEach
    void setUp() {
        guard = new AdminAccessGuard();
        sysUserRoleService = mock(SysUserRoleService.class);
        sysMenuService = mock(SysMenuService.class);
        sysConfigService = mock(SysConfigService.class);
        ipWhiteService = mock(IpWhiteService.class);
        pageFactory = mock(PageFactory.class);
        adminShellAccess = mock(AdminShellAccess.class);
        ReflectionTestUtils.setField(guard, "sysUserRoleService", sysUserRoleService);
        ReflectionTestUtils.setField(guard, "sysMenuService", sysMenuService);
        ReflectionTestUtils.setField(guard, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(guard, "ipWhiteService", ipWhiteService);
        ReflectionTestUtils.setField(guard, "pageFactory", pageFactory);
        ReflectionTestUtils.setField(guard, "adminShellAccess", adminShellAccess);
        shiroUtils = Mockito.mockStatic(ShiroUtils.class);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(new SecurityAuthConfig());
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(new SecurityOpsConfig());
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
                cn.org.autumn.modules.wall.controller.gen.IpWhiteControllerGen.class));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                cn.org.autumn.modules.usr.controller.gen.UserProfileControllerGen.class));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                cn.org.autumn.modules.safe.controller.gen.PayUserPinControllerGen.class));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                cn.org.autumn.modules.bot.controller.gen.RobotControllerGen.class));
        assertTrue(guard.shouldRequireSystemAdminForGen(
                cn.org.autumn.modules.oauth.controller.gen.ClientDetailsControllerGen.class));
        assertTrue(guard.shouldRequireSystemAdminForGen(
                cn.org.autumn.modules.im.controller.gen.ImFixtureControllerGen.class));
        assertTrue(guard.shouldRequireSystemAdminForGen(
                cn.bighub.modules.bigmodel.controller.gen.BigmodelFixtureControllerGen.class));
    }

    @Test
    void selfGenModule_spiExtendsExempt() {
        cn.org.autumn.config.AdminAccessModuleHandler imExempt =
                new cn.org.autumn.config.AdminAccessModuleHandler() {
                    @Override
                    public java.util.Set<String> selfGenModules() {
                        return java.util.Set.of("im");
                    }
                };
        ReflectionTestUtils.setField(guard, "adminAccessModuleHandlers", java.util.List.of(imExempt));
        assertFalse(guard.shouldRequireSystemAdminForGen(
                cn.org.autumn.modules.im.controller.gen.ImFixtureControllerGen.class));
        assertTrue(guard.shouldRequireSystemAdminForGen(
                cn.bighub.modules.bigmodel.controller.gen.BigmodelFixtureControllerGen.class));
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
        verify(ipWhiteService).check(eq(request), eq(AdminAccessGuard.class), eq("exec"));
    }

    @Test
    void requireOpsAdmin_rejectsNonAdminWithoutIpCheck() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(AException.class, () -> guard.requireOpsAdmin(request, getClass(), "x"));
        Mockito.verifyNoInteractions(ipWhiteService);
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
        Mockito.verifyNoInteractions(ipWhiteService);
    }

    @Test
    void webShellOps_requiresEnabledThenOpsAdmin() {
        SecurityOpsConfig ops = new SecurityOpsConfig();
        ops.setWebShellEnabled(true);
        when(sysConfigService.getSecurityOpsConfig()).thenReturn(ops);
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        guard.requireWebShellOps(request, getClass(), "ws");
        verify(ipWhiteService).check(eq(request), eq(getClass()), eq("ws"));
    }

    @Test
    void opsView_ipWhiteFailureReturns404() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin");
        when(sysUserRoleService.isSystemAdministrator("admin")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new AException("服务繁忙")).when(ipWhiteService).check(any(), any(), any());
        assertEquals("404", guard.gateView("wall", request, new MockHttpServletResponse(), new ConcurrentModel()));
    }
}
