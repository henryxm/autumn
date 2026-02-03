package cn.org.autumn.model;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Parameterized {

    Map<Integer, Class<?>> _types = new ConcurrentHashMap<>();

    default Class<?> type(int index) {
        Class<?> type = _types.get(index);
        if (null != type)
            return type;
        Class<?> clazz = getClass();
        while (true) {
            // 首先检查实现的接口中的泛型类型
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                    Type[] types = parameterizedType.getActualTypeArguments();
                    if (types.length >= index + 1 && types[index] instanceof Class) {
                        type = (Class<?>) types[index];
                        break;
                    }
                }
            }
            if (null != type)
                break;
            // 如果接口中没有找到，检查父类的泛型类型
            Type parameterized = clazz.getGenericSuperclass();
            if (parameterized instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) parameterized;
                Type[] types = parameterizedType.getActualTypeArguments();
                if (types.length >= index + 1 && types[index] instanceof Class) {
                    type = (Class<?>) types[index];
                    break;
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz == null || clazz.equals(Object.class))
                break;
        }
        _types.put(index, type);
        return type;
    }
}