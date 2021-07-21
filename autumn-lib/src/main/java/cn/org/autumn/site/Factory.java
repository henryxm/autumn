package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.*;

public class Factory {

    private static final Logger log = LoggerFactory.getLogger(InitFactory.class);

    /**
     * 查找所有实现了T类型的接口的Bean，并对Bean方法注解中指定的方法上的的Order进行排序
     * <p>
     * 根据从小到大的顺序执行Bean方法，达到根据排序进行先后有序执行的效果。
     *
     * @param t   Bean Class
     * @param mtd 方法名
     * @param <T> Bean Class
     * @return ordered Beans
     */
    public static <T> Map<Integer, List<T>> getOrdered(Class<T> t, String mtd) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return null;
        Map<String, T> map = applicationContext.getBeansOfType(t);
        Map<Integer, List<T>> ordered = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (o1 == Integer.MIN_VALUE)
                    return -1;
                return o1 - o2;
            }
        });

        for (Map.Entry<String, T> k : map.entrySet()) {
            T init = k.getValue();
            Method method = null;
            try {
                method = init.getClass().getDeclaredMethod(mtd, null);
            } catch (Exception ignored) {
            }
            Order order = null;
            if (null != method)
                order = method.getAnnotation(Order.class);
            List<T> list = null;
            int value = Integer.MAX_VALUE;
            if (null != order) {
                list = ordered.get(order.value());
                value = order.value();
            } else {
                list = ordered.get(Integer.MAX_VALUE);
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
     * 调用所有实现了接口T的Bean的方法
     * 有小到大的顺序执行
     *
     * @param t      Bean Class
     * @param method 方法名
     * @param <T>    Bean Class
     */
    public <T> void invoke(Class<T> t, String method) {
        Map<Integer, List<T>> ordered = getOrdered(t, method);
        if (null == ordered || ordered.size() == 0)
            return;
        for (Map.Entry<Integer, List<T>> entry : ordered.entrySet()) {
            List<T> list = entry.getValue();
            for (T obj : list) {
                try {
                    Method m = obj.getClass().getMethod(method, null);
                    m.invoke(obj);
                } catch (Exception e) {
                    log.debug(t.getSimpleName(), e);
                }
            }
        }
    }
}