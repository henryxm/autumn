package cn.org.autumn.site;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConfigFactory extends Factory {

    Logger log = LoggerFactory.getLogger(getClass());

    private static Map<Integer, List<ConfigFactory.Config>> map = null;

    public interface Config {
        @Order(DEFAULT_ORDER)
        void update(String key, String value);
    }

    public void update(String key, String value) {
        if (null == map)
            map = getOrdered(ConfigFactory.Config.class, "update");
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<ConfigFactory.Config>> k : map.entrySet()) {
                List<ConfigFactory.Config> configs = k.getValue();
                for (ConfigFactory.Config config : configs) {
                    try {
                        config.update(key, value);
                    } catch (Throwable t) {
                        log.error("更新配置:{},{}", key, value, t);
                    }
                }
            }
        }
    }
}
