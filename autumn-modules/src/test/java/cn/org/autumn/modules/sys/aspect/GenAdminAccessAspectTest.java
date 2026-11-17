package cn.org.autumn.modules.sys.aspect;

import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.modules.wall.controller.gen.IpWhiteControllerGen;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void delegatesToEnforceHelper() {
        IpWhiteControllerGen target = mock(IpWhiteControllerGen.class);
        JoinPoint jp = mock(JoinPoint.class);
        Signature sig = mock(Signature.class);
        when(jp.getTarget()).thenReturn(target);
        when(jp.getSignature()).thenReturn(sig);
        when(sig.getName()).thenReturn("list");
        when(jp.getArgs()).thenReturn(new Object[0]);

        aspect.requireAdminIpForManagementApi(jp);

        verify(adminAccessGuard).enforceAdminIpApiIfRequired(any(JoinPoint.class));
    }
}
