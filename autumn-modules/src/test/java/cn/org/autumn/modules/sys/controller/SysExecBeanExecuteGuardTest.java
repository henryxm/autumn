package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysExecBeanExecuteGuardTest {

    private SysExecController controller;
    private AdminAccessGuard adminAccessGuard;

    @BeforeEach
    void setUp() {
        controller = new SysExecController();
        adminAccessGuard = mock(AdminAccessGuard.class);
        ReflectionTestUtils.setField(controller, "adminAccessGuard", adminAccessGuard);
        doNothing().when(adminAccessGuard).requireOpsAdmin(any(), any(), anyString());
    }

    @Test
    void execute_rejectsWhenBeanExecuteDisabled() {
        when(adminAccessGuard.isBeanExecuteEnabled()).thenReturn(false);
        MockHttpServletRequest servlet = new MockHttpServletRequest();
        Map<String, Object> body = new HashMap<>();
        body.put("beanName", "anyBean");
        body.put("methodName", "toString");
        Map<String, Object> result = controller.executeBeanMethod(body, servlet);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Bean执行器已关闭", result.get("message"));
    }
}
