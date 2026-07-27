package cn.org.autumn.site;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminShellAccessTest {

    private AdminShellAccess access;
    private SysUserRoleService sysUserRoleService;
    private PageFactory pageFactory;
    private MockedStatic<ShiroUtils> shiroUtils;

    @BeforeEach
    void setUp() {
        access = new AdminShellAccess();
        sysUserRoleService = mock(SysUserRoleService.class);
        pageFactory = mock(PageFactory.class);
        ReflectionTestUtils.setField(access, "sysUserRoleService", sysUserRoleService);
        ReflectionTestUtils.setField(access, "pageFactory", pageFactory);
        shiroUtils = Mockito.mockStatic(ShiroUtils.class);
    }

    @AfterEach
    void tearDown() {
        if (shiroUtils != null)
            shiroUtils.close();
    }

    @Test
    void isAdminShellView_matchesBareShellNamesOnly() {
        assertTrue(AdminShellAccess.isAdminShellView("index"));
        assertTrue(AdminShellAccess.isAdminShellView("index1"));
        assertTrue(AdminShellAccess.isAdminShellView("main"));
        assertTrue(AdminShellAccess.isAdminShellView("/index.html"));
        assertFalse(AdminShellAccess.isAdminShellView("modules/bigmodel/pages/index"));
        assertFalse(AdminShellAccess.isAdminShellView("redirect:/?spm=bigmodel.index"));
        assertFalse(AdminShellAccess.isAdminShellView("404"));
    }

    @Test
    void denyUnlessSystemAdmin_passesThroughNonShell() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        assertEquals("modules/account/index", access.denyUnlessSystemAdmin("modules/account/index", request, response, model));
        verifyNoInteractions(sysUserRoleService, pageFactory);
    }

    @Test
    void denyUnlessSystemAdmin_passesThroughWhenNotLoggedIn() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        assertEquals("index", access.denyUnlessSystemAdmin("index", request, response, model));
        verifyNoInteractions(sysUserRoleService, pageFactory);
    }

    @Test
    void denyUnlessSystemAdmin_allowsSystemAdministrator() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("admin-uuid");
        when(sysUserRoleService.isSystemAdministrator("admin-uuid")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        assertEquals("index", access.denyUnlessSystemAdmin("index", request, response, model));
        verifyNoInteractions(pageFactory);
    }

    @Test
    void denyUnlessSystemAdmin_returns404ForNonAdmin() {
        shiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("user-uuid");
        when(sysUserRoleService.isSystemAdministrator("user-uuid")).thenReturn(false);
        when(pageFactory._404(any(), any(), any())).thenReturn("404");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        assertEquals("404", access.denyUnlessSystemAdmin("main", request, response, model));
        verify(pageFactory)._404(eq(request), eq(response), eq(model));
    }
}
