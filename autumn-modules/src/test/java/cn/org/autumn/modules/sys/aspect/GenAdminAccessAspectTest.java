package cn.org.autumn.modules.sys.aspect;

import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.usr.controller.gen.UserProfileControllerGen;
import cn.org.autumn.modules.wall.controller.gen.IpWhiteControllerGen;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenAdminAccessAspectTest {

    private GenAdminAccessAspect aspect;
    private AdminAccessGuard adminAccessGuard;

    @BeforeEach
    void setUp() {
        aspect = new GenAdminAccessAspect();
        adminAccessGuard = mock(AdminAccessGuard.class);
        ReflectionTestUtils.setField(aspect, "adminAccessGuard", adminAccessGuard);
    }

    @Test
    void skipsSelfModuleGen() {
        when(adminAccessGuard.shouldRequireSystemAdminForGen(UserProfileControllerGen.class)).thenReturn(false);
        JoinPoint jp = mock(JoinPoint.class);
        Signature sig = mock(Signature.class);
        when(jp.getSignature()).thenReturn(sig);
        when(sig.getDeclaringType()).thenReturn(UserProfileControllerGen.class);
        when(sig.getName()).thenReturn("list");
        aspect.requireSystemAdminForGlobalGen(jp);
        verify(adminAccessGuard, never()).requireGenAdmin(anyString());
        verify(adminAccessGuard, never()).requireSystemAdmin(anyString());
    }

    @Test
    void enforcesBusinessModuleGen() {
        when(adminAccessGuard.shouldRequireSystemAdminForGen(IpWhiteControllerGen.class)).thenReturn(true);
        JoinPoint jp = mock(JoinPoint.class);
        Signature sig = mock(Signature.class);
        when(jp.getSignature()).thenReturn(sig);
        when(sig.getDeclaringType()).thenReturn(IpWhiteControllerGen.class);
        when(sig.getName()).thenReturn("list");
        aspect.requireSystemAdminForGlobalGen(jp);
        verify(adminAccessGuard).requireGenAdmin(org.mockito.ArgumentMatchers.startsWith("gen:"));
    }
}
