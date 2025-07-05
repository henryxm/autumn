package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/sys/exec"})
public class SysExecController {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    /**
     * 获取Spring管理的所有Bean列表（分页）
     */
    @RequestMapping(value = "/beans", method = RequestMethod.GET)
    public Map<String, Object> getBeans(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String search) {
        
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return null;
        }

        ApplicationContext context = SpringContextUtils.getApplicationContext();
        String[] beanNames = context.getBeanDefinitionNames();
        
        List<Map<String, Object>> allBeans = new ArrayList<>();
        
        for (String beanName : beanNames) {
            try {
                Class<?> beanType = context.getType(beanName);
                if (beanType != null) {
                    Map<String, Object> beanInfo = new HashMap<>();
                    beanInfo.put("name", beanName);
                    beanInfo.put("type", beanType.getName());
                    beanInfo.put("simpleType", beanType.getSimpleName());
                    beanInfo.put("singleton", context.isSingleton(beanName));
                    allBeans.add(beanInfo);
                }
            } catch (Exception e) {
                // 忽略无法获取类型的Bean
            }
        }
        
        // 搜索过滤
        if (StringUtils.isNotBlank(search)) {
            allBeans = allBeans.stream()
                    .filter(bean -> {
                        String name = (String) bean.get("name");
                        String type = (String) bean.get("type");
                        String simpleType = (String) bean.get("simpleType");
                        return name.toLowerCase().contains(search.toLowerCase()) ||
                               type.toLowerCase().contains(search.toLowerCase()) ||
                               simpleType.toLowerCase().contains(search.toLowerCase());
                    })
                    .collect(Collectors.toList());
        }
        
        // 分页
        int total = allBeans.size();
        int start = (current - 1) * size;
        int end = Math.min(start + size, total);
        
        List<Map<String, Object>> pageBeans = allBeans.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageBeans);
        result.put("total", total);
        result.put("current", current);
        result.put("size", size);
        result.put("pages", (total + size - 1) / size);
        
        return result;
    }

    /**
     * 获取Bean的方法列表
     */
    @RequestMapping(value = "/bean/methods", method = RequestMethod.GET)
    public Map<String, Object> getBeanMethods(@RequestParam String beanName) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return null;
        }

        ApplicationContext context = SpringContextUtils.getApplicationContext();
        Object bean = context.getBean(beanName);
        Class<?> beanType = bean.getClass();
        
        List<Map<String, Object>> methods = new ArrayList<>();
        
        // 获取所有公共方法
        Method[] allMethods = beanType.getMethods();
        for (Method method : allMethods) {
            // 过滤掉Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("name", method.getName());
            methodInfo.put("returnType", method.getReturnType().getSimpleName());
            methodInfo.put("fullReturnType", method.getReturnType().getName());
            
            // 参数信息
            Parameter[] parameters = method.getParameters();
            List<Map<String, Object>> paramList = new ArrayList<>();
            for (Parameter param : parameters) {
                Map<String, Object> paramInfo = new HashMap<>();
                paramInfo.put("name", param.getName());
                paramInfo.put("type", param.getType().getSimpleName());
                paramInfo.put("fullType", param.getType().getName());
                paramInfo.put("required", true);
                paramList.add(paramInfo);
            }
            methodInfo.put("parameters", paramList);
            methodInfo.put("parameterCount", parameters.length);
            
            methods.add(methodInfo);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("beanName", beanName);
        result.put("beanType", beanType.getName());
        result.put("methods", methods);
        
        return result;
    }

    /**
     * 执行Bean方法
     */
    @RequestMapping(value = "/bean/execute", method = RequestMethod.POST)
    public Map<String, Object> executeBeanMethod(@RequestBody Map<String, Object> request) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        
        try {
            String beanName = (String) request.get("beanName");
            String methodName = (String) request.get("methodName");
            List<Object> parameters = (List<Object>) request.get("parameters");
            
            ApplicationContext context = SpringContextUtils.getApplicationContext();
            Object bean = context.getBean(beanName);
            Class<?> beanType = bean.getClass();
            
            // 查找方法
            Method targetMethod = null;
            Method[] methods = beanType.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == parameters.size()) {
                    // 进一步检查参数类型匹配
                    Parameter[] methodParams = method.getParameters();
                    boolean typeMatch = true;
                    for (int i = 0; i < parameters.size(); i++) {
                        Object param = parameters.get(i);
                        if (param != null && !methodParams[i].getType().isAssignableFrom(param.getClass())) {
                            // 如果不是直接匹配，尝试转换
                            try {
                                convertParameter(param, methodParams[i].getType());
                            } catch (Exception e) {
                                typeMatch = false;
                                break;
                            }
                        }
                    }
                    if (typeMatch) {
                        targetMethod = method;
                        break;
                    }
                }
            }
            
            if (targetMethod == null) {
                result.put("success", false);
                result.put("message", "方法未找到或参数数量不匹配");
                return result;
            }
            
            // 转换参数类型
            Object[] convertedParams = new Object[parameters.size()];
            Parameter[] methodParams = targetMethod.getParameters();
            
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                Class<?> paramType = methodParams[i].getType();
                convertedParams[i] = convertParameter(param, paramType);
            }
            
            // 执行方法
            Object returnValue = targetMethod.invoke(bean, convertedParams);
            
            result.put("success", true);
            result.put("returnValue", returnValue);
            result.put("returnType", targetMethod.getReturnType().getName());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行失败: " + e.getMessage());
            result.put("exception", e.getClass().getSimpleName());
        }
        
        return result;
    }

    /**
     * 参数类型转换
     */
    private Object convertParameter(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        String stringValue = value.toString();
        
        if (targetType == String.class) {
            return stringValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(stringValue);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(stringValue);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(stringValue);
        } else if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(stringValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(stringValue);
        } else if (targetType == Short.class || targetType == short.class) {
            return Short.parseShort(stringValue);
        } else if (targetType == Byte.class || targetType == byte.class) {
            return Byte.parseByte(stringValue);
        } else if (targetType == Character.class || targetType == char.class) {
            return stringValue.charAt(0);
        }
        
        return value;
    }
}
