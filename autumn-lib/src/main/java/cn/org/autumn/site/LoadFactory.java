package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoadFactory {

    public interface Load {
        void load();
    }

    public void load() {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return;
        Map<String, Load> map = applicationContext.getBeansOfType(Load.class);
        for (Map.Entry<String, Load> k : map.entrySet()) {
            Load load = k.getValue();
            try {
                load.load();
            } catch (Exception e) {
            }
        }
    }
}
