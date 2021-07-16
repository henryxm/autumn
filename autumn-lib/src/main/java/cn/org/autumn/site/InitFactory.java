package cn.org.autumn.site;

import cn.org.autumn.table.TableInit;
import cn.org.autumn.utils.SpringContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class InitFactory {
    private static final Logger log = LoggerFactory.getLogger(InitFactory.class);

    @Autowired
    private TableInit tableInit;

    //初始化
    public interface Init {
        void init();
    }

    //在初始化数据前执行
    public interface Before {
        void before();
    }

    //在初始化完成后执行
    public interface After {
        void after();
    }

    public void init() {
        if (!tableInit.init)
            return;
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return;

        Map<String, Before> before = applicationContext.getBeansOfType(Before.class);
        for (Map.Entry<String, Before> entry : before.entrySet()) {
            try {
                Before init = entry.getValue();
                init.before();
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
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
                    log.debug(init.getClass().getSimpleName(), e);
                }
            }
        }

        Map<String, After> after = applicationContext.getBeansOfType(After.class);
        for (Map.Entry<String, After> entry : after.entrySet()) {
            try {
                After init = entry.getValue();
                init.after();
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
    }
}