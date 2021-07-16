package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoadFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public interface Before {
        void before();
    }

    public interface After {
        void after();
    }

    public interface Load {
        void load();
    }

    public void load() {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return;

        Map<String, Before> before = applicationContext.getBeansOfType(Before.class);
        for (Map.Entry<String, Before> k : before.entrySet()) {
            Before b = k.getValue();
            try {
                b.before();
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }

        Map<String, Load> map = applicationContext.getBeansOfType(Load.class);
        for (Map.Entry<String, Load> k : map.entrySet()) {
            Load load = k.getValue();
            try {
                load.load();
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }

        Map<String, After> after = applicationContext.getBeansOfType(After.class);
        for (Map.Entry<String, After> k : after.entrySet()) {
            After a = k.getValue();
            try {
                a.after();
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
    }
}
