package cn.org.autumn.modules.sys.controller;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class SysExecControllerTest {

    @Test
    public void testParameterConversion() {
        // 仅保留纯函数能力测试，避免静态上下文依赖导致构建环境不稳定
        SysExecController controller = new SysExecController();
        try {
            Method convertMethod = SysExecController.class.getDeclaredMethod("convertParameter", Object.class, Class.class);
            convertMethod.setAccessible(true);
            Object result = convertMethod.invoke(controller, "123", Integer.class);
            assertEquals(123, result);
            result = convertMethod.invoke(controller, "456", Long.class);
            assertEquals(456L, result);
            result = convertMethod.invoke(controller, "true", Boolean.class);
            assertEquals(true, result);
            result = convertMethod.invoke(controller, null, String.class);
            assertNull(result);
        } catch (Exception e) {
            fail("测试参数转换失败: " + e.getMessage());
        }
    }
} 