package cn.org.autumn.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Parameterized {

    Map<String, Class<?>> _types = new ConcurrentHashMap<>();

    default Class<?> type(int index) {
        return type(index, null);
    }

    default <T extends Annotation> Class<?> type(int index, Class<T> annotation) {
        Class<?> clazz = getClass();
        String key = clazz.getName() + "." + index;
        Class<?> type = _types.get(key);
        if (null != type)
            return type;
        while (true) {
            // 首先检查实现的接口中的泛型类型
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                    Type[] types = parameterizedType.getActualTypeArguments();
                    if (types.length >= index + 1 && types[index] instanceof Class) {
                        Class<?> tmp = (Class<?>) types[index];
                        if (null == annotation)
                            type = tmp;
                        else {
                            T table = tmp.getAnnotation(annotation);
                            if (null != table) {
                                type = tmp;
                                break;
                            }
                        }
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
                    Class<?> tmp = (Class<?>) types[index];
                    if (null == annotation)
                        type = tmp;
                    else {
                        T table = tmp.getAnnotation(annotation);
                        if (null != table) {
                            type = tmp;
                            break;
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz == null || clazz.equals(Object.class))
                break;
        }
        _types.put(key, type);
        return type;
    }
}