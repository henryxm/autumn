package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.utils.SpringContextUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SysExecControllerTest {

    @InjectMocks
    private SysExecController sysExecController;

    @Mock
    private SysUserRoleService sysUserRoleService;

    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        // 模拟SpringContextUtils
        try (MockedStatic<SpringContextUtils> mockedSpringContext = mockStatic(SpringContextUtils.class);
             MockedStatic<ShiroUtils> mockedShiroUtils = mockStatic(ShiroUtils.class)) {
            mockedSpringContext.when(SpringContextUtils::getApplicationContext).thenReturn(applicationContext);
            mockedShiroUtils.when(ShiroUtils::isLogin).thenReturn(true);
            mockedShiroUtils.when(ShiroUtils::getUserUuid).thenReturn("test-user");
        }
    }

    @Test
    public void testGetBeans_WhenNotLoggedIn_ShouldReturnNull() {
        // 模拟未登录状态
        try (MockedStatic<ShiroUtils> mockedShiroUtils = mockStatic(ShiroUtils.class)) {
            mockedShiroUtils.when(ShiroUtils::isLogin).thenReturn(false);
            Map<String, Object> result = sysExecController.getBeans(1, 50, null, null);
            assertNull(result);
        }
    }

    @Test
    public void testGetBeans_WhenNotAdmin_ShouldReturnNull() {
        // 模拟非管理员状态
        when(sysUserRoleService.isSystemAdministrator(anyString())).thenReturn(false);
        Map<String, Object> result = sysExecController.getBeans(1, 50, null, null);
        assertNull(result);
    }

    @Test
    public void testGetBeanMethods_WhenNotLoggedIn_ShouldReturnNull() {
        // 模拟未登录状态
        try (MockedStatic<ShiroUtils> mockedShiroUtils = mockStatic(ShiroUtils.class)) {
            mockedShiroUtils.when(ShiroUtils::isLogin).thenReturn(false);
            Map<String, Object> result = sysExecController.getBeanMethods("testBean", null);
            assertNull(result);
        }
    }

    @Test
    public void testExecuteBeanMethod_WhenNotLoggedIn_ShouldReturnNull() {
        // 模拟未登录状态
        try (MockedStatic<ShiroUtils> mockedShiroUtils = mockStatic(ShiroUtils.class)) {
            mockedShiroUtils.when(ShiroUtils::isLogin).thenReturn(false);
            Map<String, Object> request = new HashMap<>();
            request.put("beanName", "testBean");
            request.put("methodName", "testMethod");
            request.put("parameters", new Object[0]);
            Map<String, Object> result = sysExecController.executeBeanMethod(request, null);
            assertNull(result);
        }
    }

    @Test
    public void testParameterConversion() {
        // 测试参数转换功能
        SysExecController controller = new SysExecController();
        // 使用反射访问私有方法
        try {
            Method convertMethod = SysExecController.class.getDeclaredMethod("convertParameter", Object.class, Class.class);
            convertMethod.setAccessible(true);
            // 测试字符串转换
            Object result = convertMethod.invoke(controller, "123", Integer.class);
            assertEquals(123, result);
            // 测试数字转换
            result = convertMethod.invoke(controller, "456", Long.class);
            assertEquals(456L, result);
            // 测试布尔转换
            result = convertMethod.invoke(controller, "true", Boolean.class);
            assertEquals(true, result);
            // 测试空值
            result = convertMethod.invoke(controller, null, String.class);
            assertNull(result);
        } catch (Exception e) {
            fail("测试参数转换失败: " + e.getMessage());
        }
    }
} 