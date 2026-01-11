package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.utils.SpringContextUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/sys/exec"})
public class SysExecController {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired(required = false)
    private Gson gson;

    /**
     * 获取Spring管理的所有Bean列表（分页）
     */
    @RequestMapping(value = "/beans", method = RequestMethod.GET)
    public Map<String, Object> getBeans(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "50") Integer size, @RequestParam(required = false) String search) {
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
                        return name.toLowerCase().contains(search.toLowerCase()) || type.toLowerCase().contains(search.toLowerCase()) || simpleType.toLowerCase().contains(search.toLowerCase());
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
                                    } else if (paramType == convertedType) {
                                        // 类型完全相等（如BigDecimal == BigDecimal）
                                        matchScore += 2; // 完全匹配，分数更高
                                    } else if (paramType.isAssignableFrom(convertedType)) {
                                        matchScore += 1;
                                    } else if (isNumberTypeCompatible(paramType, convertedType)) {
                                        // 数字类型兼容性检查（如Integer可以转换为BigDecimal）
                                        matchScore += 1;
                                    } else {
                                        allParamsMatch = false;
                                        errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                                .append(")类型不匹配: 期望").append(paramType.getSimpleName())
                                                .append("，实际").append(convertedType.getSimpleName()).append("; ");
                                        break;
                                    }
                                } else {
                                    // 转换后为null，检查目标类型是否允许null
                                    if (paramType.isPrimitive()) {
                                        allParamsMatch = false;
                                        errorMsg.append("参数").append(i).append("(").append(methodParams[i].getName())
                                                .append(")转换后为null，但目标类型是基本类型").append(paramType.getSimpleName()).append("; ");
                                        break;
                                    }
                                    // 非基本类型允许null
                                    matchScore += 1;
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
                try {
                    convertedParams[i] = convertParameter(param, paramType);
                } catch (Exception e) {
                    // 参数转换失败，提供详细的错误信息
                    String paramName = methodParams[i].getName();
                    String paramTypeName = paramType.getSimpleName();
                    String actualType = param != null ? param.getClass().getSimpleName() : "null";
                    String paramValue = param != null ? param.toString() : "null";
                    throw new IllegalArgumentException(String.format("参数 %d (%s) 类型转换失败: 期望 %s，实际 %s，值: %s。错误: %s", i, paramName, paramTypeName, actualType, paramValue.length() > 50 ? paramValue.substring(0, 50) + "..." : paramValue, e.getMessage()), e);
                }
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
     * <p>
     * 支持的类型转换：
     * 1. 基本类型和包装类型（Integer、Long、Double、Float、Boolean等）
     * 2. BigDecimal - 从String或Number转换
     * 3. Date - 从String（日期格式）或Long（时间戳）转换
     * 4. List/Set/Collection - 从JSON数组转换
     * 5. Map - 从JSON对象转换
     * 6. 其他类型 - 尝试使用Gson进行JSON反序列化
     */
    private Object convertParameter(Object value, Class<?> targetType) {
        if (value == null) {
            // null值处理
            if (targetType.isPrimitive()) {
                throw new IllegalArgumentException("无法将null转换为基本类型: " + targetType.getSimpleName());
            }
            return null;
        }
        // 如果已经是目标类型，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        String stringValue = value.toString().trim();
        // 处理基本类型和包装类型
        if (targetType == String.class) {
            return stringValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? 0 : null;
            }
            return Integer.parseInt(stringValue);
        } else if (targetType == Long.class || targetType == long.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? 0L : null;
            }
            return Long.parseLong(stringValue);
        } else if (targetType == Double.class || targetType == double.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? 0.0 : null;
            }
            return Double.parseDouble(stringValue);
        } else if (targetType == Float.class || targetType == float.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? 0.0f : null;
            }
            return Float.parseFloat(stringValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? false : null;
            }
            // 支持多种布尔值格式
            String lower = stringValue.toLowerCase();
            if ("true".equals(lower) || "1".equals(lower) || "yes".equals(lower)) {
                return true;
            } else if ("false".equals(lower) || "0".equals(lower) || "no".equals(lower)) {
                return false;
            }
            return Boolean.parseBoolean(stringValue);
        } else if (targetType == Short.class || targetType == short.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? (short) 0 : null;
            }
            return Short.parseShort(stringValue);
        } else if (targetType == Byte.class || targetType == byte.class) {
            if (stringValue.isEmpty()) {
                return targetType.isPrimitive() ? (byte) 0 : null;
            }
            return Byte.parseByte(stringValue);
        } else if (targetType == Character.class || targetType == char.class) {
            if (stringValue.isEmpty()) {
                throw new IllegalArgumentException("无法将空字符串转换为字符");
            }
            return stringValue.charAt(0);
        }
        // 处理BigDecimal
        if (targetType == BigDecimal.class) {
            if (stringValue.isEmpty()) {
                return null;
            }
            try {
                // 如果value是Number类型，直接转换
                if (value instanceof Number) {
                    return new BigDecimal(value.toString());
                }
                return new BigDecimal(stringValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无法将 \"" + stringValue + "\" 转换为 BigDecimal: " + e.getMessage());
            }
        }
        // 处理Date类型
        if (targetType == Date.class || targetType == java.sql.Date.class) {
            if (stringValue.isEmpty()) {
                return null;
            }
            try {
                // 尝试解析为时间戳（毫秒）
                if (stringValue.matches("^\\d+$")) {
                    long timestamp = Long.parseLong(stringValue);
                    Date date = new Date(timestamp);
                    if (targetType == java.sql.Date.class) {
                        return new java.sql.Date(date.getTime());
                    }
                    return date;
                }
                // 尝试解析为日期字符串
                if (gson != null) {
                    // 使用Gson的Date适配器解析
                    JsonElement jsonElement = JsonParser.parseString("\"" + stringValue + "\"");
                    Date date = gson.fromJson(jsonElement, Date.class);
                    if (targetType == java.sql.Date.class) {
                        return new java.sql.Date(date.getTime());
                    }
                    return date;
                }
                // 如果没有Gson，尝试常见日期格式
                String[] patterns = {
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd",
                        "yyyy/MM/dd HH:mm:ss",
                        "yyyy/MM/dd"
                };
                for (String pattern : patterns) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                        Date date = sdf.parse(stringValue);
                        if (targetType == java.sql.Date.class) {
                            return new java.sql.Date(date.getTime());
                        }
                        return date;
                    } catch (ParseException ignored) {
                        // 继续尝试下一个格式
                    }
                }
                throw new IllegalArgumentException("无法解析日期字符串: " + stringValue);
            } catch (Exception e) {
                throw new IllegalArgumentException("无法将 \"" + stringValue + "\" 转换为 Date: " + e.getMessage());
            }
        }
        // 处理List/Set/Collection类型
        if (Collection.class.isAssignableFrom(targetType)) {
            if (stringValue.isEmpty()) {
                return createEmptyCollection(targetType);
            }
            try {
                // 如果value已经是Collection类型，尝试转换
                if (value instanceof Collection) {
                    Collection<?> source = (Collection<?>) value;
                    Collection<Object> target = createEmptyCollection(targetType);
                    target.addAll(source);
                    return target;
                }
                // 尝试解析JSON数组
                if (gson != null && (stringValue.startsWith("[") || stringValue.startsWith("{"))) {
                    JsonElement jsonElement = JsonParser.parseString(stringValue);
                    if (jsonElement.isJsonArray()) {
                        // 获取泛型类型（如果可能）
                        Type listType = TypeToken.getParameterized(targetType, Object.class).getType();
                        Object result = gson.fromJson(jsonElement, listType);
                        if (targetType.isAssignableFrom(result.getClass())) {
                            return result;
                        }
                    }
                }
                // 如果解析失败，尝试按逗号分割
                String[] parts = stringValue.split(",");
                Collection<Object> result = createEmptyCollection(targetType);
                for (String part : parts) {
                    result.add(part.trim());
                }
                return result;
            } catch (Exception e) {
                throw new IllegalArgumentException("无法将 \"" + stringValue + "\" 转换为 " + targetType.getSimpleName() + ": " + e.getMessage());
            }
        }
        // 处理Map类型
        if (Map.class.isAssignableFrom(targetType)) {
            if (stringValue.isEmpty()) {
                return new HashMap<>();
            }
            try {
                // 如果value已经是Map类型，直接返回
                if (value instanceof Map) {
                    return value;
                }
                // 尝试解析JSON对象
                if (gson != null && stringValue.startsWith("{")) {
                    JsonElement jsonElement = JsonParser.parseString(stringValue);
                    if (jsonElement.isJsonObject()) {
                        Type mapType = TypeToken.getParameterized(targetType, String.class, Object.class).getType();
                        return gson.fromJson(jsonElement, mapType);
                    }
                }
                throw new IllegalArgumentException("无法将 \"" + stringValue + "\" 转换为 Map，需要JSON对象格式");
            } catch (Exception e) {
                throw new IllegalArgumentException("无法将 \"" + stringValue + "\" 转换为 " + targetType.getSimpleName() + ": " + e.getMessage());
            }
        }
        // 处理数组类型
        if (targetType.isArray()) {
            if (stringValue.isEmpty()) {
                return java.lang.reflect.Array.newInstance(targetType.getComponentType(), 0);
            }
            try {
                // 如果value已经是数组，尝试转换
                if (value.getClass().isArray()) {
                    int length = java.lang.reflect.Array.getLength(value);
                    Object targetArray = java.lang.reflect.Array.newInstance(targetType.getComponentType(), length);
                    for (int i = 0; i < length; i++) {
                        Object element = java.lang.reflect.Array.get(value, i);
                        Object converted = convertParameter(element, targetType.getComponentType());
                        java.lang.reflect.Array.set(targetArray, i, converted);
                    }
                    return targetArray;
                }
                // 尝试解析JSON数组
                if (gson != null && stringValue.startsWith("[")) {
                    JsonElement jsonElement = JsonParser.parseString(stringValue);
                    if (jsonElement.isJsonArray()) {
                        List<?> list = gson.fromJson(jsonElement, TypeToken.getParameterized(List.class, targetType.getComponentType()).getType());
                        Object array = java.lang.reflect.Array.newInstance(targetType.getComponentType(), list.size());
                        for (int i = 0; i < list.size(); i++) {
                            java.lang.reflect.Array.set(array, i, list.get(i));
                        }
                        return array;
                    }
                }
                // 如果解析失败，尝试按逗号分割
                String[] parts = stringValue.split(",");
                Object array = java.lang.reflect.Array.newInstance(targetType.getComponentType(), parts.length);
                for (int i = 0; i < parts.length; i++) {
                    Object converted = convertParameter(parts[i].trim(), targetType.getComponentType());
                    java.lang.reflect.Array.set(array, i, converted);
                }
                return array;
            } catch (Exception e) {
                throw new IllegalArgumentException("无法将 \"" + stringValue + "\" 转换为数组类型 " + targetType.getSimpleName() + ": " + e.getMessage());
            }
        }
        // 尝试使用Gson进行JSON反序列化（适用于复杂对象）
        if (gson != null) {
            try {
                // 如果value是字符串且看起来像JSON
                if (value instanceof String && (stringValue.startsWith("{") || stringValue.startsWith("["))) {
                    return gson.fromJson(stringValue, targetType);
                }
                // 如果value是Map，尝试转换为目标类型
                if (value instanceof Map) {
                    String json = gson.toJson(value);
                    return gson.fromJson(json, targetType);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("使用Gson转换类型失败: {} -> {}, 错误: {}", value.getClass().getName(), targetType.getName(), e.getMessage());
                }
                // 继续尝试其他方式
            }
        }
        // 如果所有转换都失败，抛出异常
        throw new IllegalArgumentException("无法将类型 " + value.getClass().getSimpleName() + " 转换为 " + targetType.getSimpleName() + "，值: " + (stringValue.length() > 100 ? stringValue.substring(0, 100) + "..." : stringValue));
    }

    /**
     * 创建空的集合实例
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> createEmptyCollection(Class<?> collectionType) {
        if (collectionType == List.class || collectionType == ArrayList.class) {
            return new ArrayList<>();
        } else if (collectionType == Set.class || collectionType == HashSet.class) {
            return new HashSet<>();
        } else if (collectionType == LinkedList.class) {
            return new LinkedList<>();
        } else if (collectionType == Vector.class) {
            return new Vector<>();
        } else {
            // 尝试使用默认构造函数创建
            try {
                return (Collection<Object>) collectionType.newInstance();
            } catch (Exception e) {
                // 如果失败，返回ArrayList作为默认值
                return new ArrayList<>();
            }
        }
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

    /**
     * 检查数字类型兼容性
     * <p>
     * 例如：Integer、Long、Double等可以转换为BigDecimal
     * BigDecimal可以接受任何Number类型
     */
    private boolean isNumberTypeCompatible(Class<?> targetType, Class<?> sourceType) {
        // BigDecimal可以接受任何Number类型
        if (targetType == BigDecimal.class && Number.class.isAssignableFrom(sourceType)) {
            return true;
        }
        // 其他数字类型之间的兼容性
        if (Number.class.isAssignableFrom(targetType) && Number.class.isAssignableFrom(sourceType)) {
            // 如果目标类型是更宽泛的数字类型，可以接受
            if (targetType == Number.class) {
                return true;
            }
            // BigDecimal可以接受任何Number
            if (targetType == BigDecimal.class) {
                return true;
            }
        }
        return false;
    }
}