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
     * 对于重名方法，通过参数类型和数量进行区分
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
            StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Map<String, Object> paramInfo = new HashMap<>();
                paramInfo.put("name", param.getName());
                paramInfo.put("type", param.getType().getSimpleName());
                paramInfo.put("fullType", param.getType().getName());
                paramInfo.put("required", true);
                paramList.add(paramInfo);
                
                if (i > 0) {
                    methodSignature.append(", ");
                }
                methodSignature.append(param.getType().getSimpleName());
            }
            methodSignature.append(")");
            
            methodInfo.put("parameters", paramList);
            methodInfo.put("parameterCount", parameters.length);
            methodInfo.put("signature", methodSignature.toString()); // 方法签名，用于区分重名方法
            methodInfo.put("methodId", method.getName() + "_" + methodSignature.toString()); // 唯一标识
            
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
            @SuppressWarnings("unchecked")
            List<Object> parameters = (List<Object>) request.get("parameters");
            
            ApplicationContext context = SpringContextUtils.getApplicationContext();
            Object bean = context.getBean(beanName);
            Class<?> beanType = bean.getClass();
            
            // 查找方法 - 精确匹配参数类型和数量
            Method targetMethod = null;
            List<Method> candidateMethods = new ArrayList<>();
            Method[] methods = beanType.getMethods();
            
            // 第一步：筛选出方法名相同且参数数量匹配的方法
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == parameters.size()) {
                    candidateMethods.add(method);
                }
            }
            
            if (candidateMethods.isEmpty()) {
                result.put("success", false);
                result.put("message", "方法未找到或参数数量不匹配");
                return result;
            }
            
            // 第二步：精确匹配参数类型
            // 如果有多个候选方法，需要找到最匹配的一个
            Method bestMatch = null;
            int bestMatchScore = -1;
            List<String> errorMessages = new ArrayList<>();
            
            for (Method method : candidateMethods) {
                Parameter[] methodParams = method.getParameters();
                boolean allParamsMatch = true;
                int matchScore = 0; // 匹配分数：完全匹配=2，可转换=1，不匹配=0
                StringBuilder errorMsg = new StringBuilder();
                
                for (int i = 0; i < parameters.size(); i++) {
                    Object param = parameters.get(i);
                    Class<?> paramType = methodParams[i].getType();
                    
                    if (param == null) {
                        // null值可以匹配任何非基本类型
                        if (paramType.isPrimitive()) {
                            allParamsMatch = false;
                            errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                    .append(")不能为null，类型为基本类型").append(paramType.getSimpleName()).append("; ");
                            break;
                        }
                        matchScore += 1; // null值匹配
                    } else {
                        Class<?> actualParamType = param.getClass();
                        
                        // 完全类型匹配
                        if (paramType.isAssignableFrom(actualParamType)) {
                            matchScore += 2;
                        } else {
                            // 尝试类型转换
                            try {
                                Object converted = convertParameter(param, paramType);
                                if (converted != null) {
                                    // 验证转换后的类型是否匹配
                                    Class<?> convertedType = converted.getClass();
                                    if (paramType.isPrimitive()) {
                                        // 基本类型需要特殊处理
                                        if (isPrimitiveWrapperMatch(paramType, convertedType)) {
                                            matchScore += 1;
                                        } else {
                                            allParamsMatch = false;
                                            errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                                    .append(")类型不匹配: 期望").append(paramType.getSimpleName())
                                                    .append("，实际").append(convertedType.getSimpleName()).append("; ");
                                            break;
                                        }
                                    } else if (paramType.isAssignableFrom(convertedType)) {
                                        matchScore += 1;
                                    } else {
                                        allParamsMatch = false;
                                        errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                                .append(")类型不匹配: 期望").append(paramType.getSimpleName())
                                                .append("，实际").append(convertedType.getSimpleName()).append("; ");
                                        break;
                                    }
                                } else {
                                    allParamsMatch = false;
                                    errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                            .append(")转换后为null; ");
                                    break;
                                }
                            } catch (Exception e) {
                                allParamsMatch = false;
                                errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                        .append(")转换失败: ").append(e.getMessage()).append("; ");
                                break;
                            }
                        }
                    }
                }
                
                if (allParamsMatch && matchScore > bestMatchScore) {
                    bestMatch = method;
                    bestMatchScore = matchScore;
                } else if (!allParamsMatch) {
                    errorMessages.add(method.getName() + method.getParameterCount() + "参数: " + errorMsg.toString());
                }
            }
            
            if (bestMatch == null) {
                result.put("success", false);
                StringBuilder msg = new StringBuilder("方法未找到或参数类型不匹配");
                if (!errorMessages.isEmpty()) {
                    msg.append("。候选方法错误: ").append(String.join("; ", errorMessages));
                }
                result.put("message", msg.toString());
                return result;
            }
            
            targetMethod = bestMatch;
            
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
        
        // 如果已经是目标类型，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        String stringValue = value.toString();
        
        // 处理基本类型和包装类型
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
            if (stringValue.length() > 0) {
                return stringValue.charAt(0);
            }
            throw new IllegalArgumentException("无法将空字符串转换为字符");
        }
        
        return value;
    }
    
    /**
     * 检查基本类型和包装类型是否匹配
     */
    private boolean isPrimitiveWrapperMatch(Class<?> primitiveType, Class<?> wrapperType) {
        if (primitiveType == int.class && wrapperType == Integer.class) return true;
        if (primitiveType == long.class && wrapperType == Long.class) return true;
        if (primitiveType == double.class && wrapperType == Double.class) return true;
        if (primitiveType == float.class && wrapperType == Float.class) return true;
        if (primitiveType == boolean.class && wrapperType == Boolean.class) return true;
        if (primitiveType == short.class && wrapperType == Short.class) return true;
        if (primitiveType == byte.class && wrapperType == Byte.class) return true;
        if (primitiveType == char.class && wrapperType == Character.class) return true;
        return false;
    }
}
