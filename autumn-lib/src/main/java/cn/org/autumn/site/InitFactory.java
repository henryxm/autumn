package cn.org.autumn.site;

import cn.org.autumn.table.TableInit;
import cn.org.autumn.utils.SpringContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class InitFactory {

    @Autowired
    private TableInit tableInit;

    public interface Init {
        void init();
    }

    public void init() {
        if (!tableInit.init)
            return;
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return;
        Map<String, Init> map = applicationContext.getBeansOfType(Init.class);
        Map<Integer, List<Init>> ordered = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (o1 == Integer.MIN_VALUE)
                    return -1;
                return o1 - o2;
            }
        });

        for (Map.Entry<String, Init> k : map.entrySet()) {
            Init init = k.getValue();
            Method method = null;
            try {
                method = init.getClass().getDeclaredMethod("init", null);
            } catch (Exception e) {
            }
            Order order = null;
            if (null != method)
                order = method.getAnnotation(Order.class);
            List<Init> list = null;
            Integer value = Integer.MAX_VALUE;
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

        for (Map.Entry<Integer, List<Init>> entry : ordered.entrySet()) {
            List<Init> list = entry.getValue();
            for (Init init : list) {
                try {
                    init.init();
                } catch (Exception e) {
                }
            }
        }
    }
}
