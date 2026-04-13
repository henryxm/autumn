package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author mac
 */
@Slf4j
public class Factory {

    /**
     * 排序缺省值
     */
    public static final int DEFAULT_ORDER = Integer.MAX_VALUE / 1000;

    /**
     * 查找所有实现了T类型的接口的Bean，并对Bean方法注解中指定的方法上的的Order进行排序
     * <p>
     * 根据从小到大的顺序执行Bean方法，达到根据排序进行先后有序执行的效果。
     *
     * @param t    Bean Class
     * @param name 方法名
     * @param <T>  Bean Class
     * @return ordered Beans
     */
    public static <T> Map<Integer, List<T>> getOrdered(Class<T> t, String name, Class<?>... parameterTypes) {
        return getOrdered(SpringContextUtils.getApplicationContext(), t, name, parameterTypes);
    }

    /**
     * 与 {@link #getOrdered(Class, String, Class[])} 相同，但使用指定的 {@link ApplicationContext}
     * （用于上下文即将关闭、不能依赖 {@link SpringContextUtils} 的场景）。
     */
    public static <T> Map<Integer, List<T>> getOrdered(ApplicationContext applicationContext, Class<T> t, String name, Class<?>... parameterTypes) {
        if (null == applicationContext) {
            return null;
        }
        Map<String, T> map = applicationContext.getBeansOfType(t);
        Map<Integer, List<T>> ordered = new TreeMap<>((o1, o2) -> {
            if (o1 == Integer.MIN_VALUE) {
                return -1;
            }
            return o1 - o2;
        });

        for (Map.Entry<String, T> k : map.entrySet()) {
            T init = k.getValue();
            Method method;
            Order order = null;

            //获取实例中的排序Order，如果没找到，你从超类中寻找
            Class<?> clazz = init.getClass();
            try {
                while (null != clazz && null == order && !clazz.equals(Object.class)) {
                    if (StringUtils.isNotBlank(name)) {
                        method = clazz.getMethod(name, parameterTypes);
                        order = method.getAnnotation(Order.class);
                    }
                    if (null == order) {
                        order = clazz.getAnnotation(Order.class);
                    }
                    clazz = clazz.getSuperclass();
                }
            } catch (Exception ignored) {
            }
            //如果从实例中没有找到Order，则寻找接口中的Order
            if (null == order) {
                try {
                    if (StringUtils.isNotBlank(name)) {
                        method = t.getMethod(name, parameterTypes);
                        order = method.getAnnotation(Order.class);
                    }
                    if (null == order) {
                        order = t.getAnnotation(Order.class);
                    }
                } catch (Exception ignored) {
                }
            }
            List<T> list;
            int value = Integer.MAX_VALUE / 100;
            if (null != order) {
                list = ordered.get(order.value());
                value = order.value();
            } else {
                list = ordered.get(value);
            }
            if (null == list) {
                list = new ArrayList<>();
                ordered.put(value, list);
            }
            list.add(init);
        }
        return ordered;
    }

    /**
     * 调用所有实现了接口 T 的 Bean 的方法，按 {@link Order} 升序。
     */
    public static <T> void invoke(ApplicationContext applicationContext, Class<T> t, String name) {
        Map<Integer, List<T>> ordered = getOrdered(applicationContext, t, name);
        if (null == ordered || ordered.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, List<T>> entry : ordered.entrySet()) {
            List<T> list = entry.getValue();
            for (T obj : list) {
                try {
                    Method m = obj.getClass().getMethod(name);
                    m.invoke(obj);
                } catch (Exception e) {
                    log.error("{}.{}", obj.getClass().getSimpleName(), name, e);
                }
            }
        }
    }

    /**
     * 调用所有实现了接口T的Bean的方法
     * 有小到大的顺序执行
     *
     * @param t    Bean Class
     * @param name 方法名
     * @param <T>  Bean Class
     */
    public <T> void invoke(Class<T> t, String name) {
        invoke(SpringContextUtils.getApplicationContext(), t, name);
    }

    public static <T> List<T> getOrderList(ApplicationContext applicationContext, Class<T> t, String name, Class<?>... parameterTypes) {
        List<T> tmp = new ArrayList<>();
        Map<Integer, List<T>> ordered = getOrdered(applicationContext, t, name, parameterTypes);
        if (null != ordered && !ordered.isEmpty()) {
            for (Map.Entry<Integer, List<T>> entry : ordered.entrySet()) {
                List<T> list = entry.getValue();
                tmp.addAll(list);
            }
        }
        return tmp;
    }

    public <T> List<T> getOrderList(Class<T> t) {
        return getOrderList(t, null);
    }

    public <T> List<T> getOrderList(Class<T> t, String name, Class<?>... parameterTypes) {
        return getOrderList(SpringContextUtils.getApplicationContext(), t, name, parameterTypes);
    }
}