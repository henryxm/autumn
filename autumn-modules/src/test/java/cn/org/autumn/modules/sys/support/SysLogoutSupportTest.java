package cn.org.autumn.modules.sys.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SysLogoutSupportTest {

    private MockedStatic<ShiroUtils> shiroUtils;
    private Subject subject;

    @BeforeEach
    void setUp() {
        SecurityManager securityManager = mock(SecurityManager.class);
        subject = mock(Subject.class);
        SecurityUtils.setSecurityManager(securityManager);
        ThreadContext.bind(subject);
        shiroUtils = Mockito.mockStatic(ShiroUtils.class);
    }

    @AfterEach
    void tearDown() {
        if (shiroUtils != null) {
            shiroUtils.close();
        }
        ThreadContext.unbindSubject();
        ThreadContext.remove();
    }

    @Test
    void marksForceLogoutBeforeShiroLogout() {
        ShiroSessionService sessionService = mock(ShiroSessionService.class);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn("user-uuid-1");
        SysLogoutSupport.logoutAndForceReauth(sessionService);
        verify(sessionService).markForceLogoutForUser("user-uuid-1");
        shiroUtils.verify(ShiroUtils::logout);
    }

    @Test
    void skipsForceMarkWhenNoUser() {
        ShiroSessionService sessionService = mock(ShiroSessionService.class);
        shiroUtils.when(ShiroUtils::getUserUuid).thenReturn(null);
        SysLogoutSupport.logoutAndForceReauth(sessionService);
        verifyNoInteractions(sessionService);
        shiroUtils.verify(ShiroUtils::logout);
    }
}
