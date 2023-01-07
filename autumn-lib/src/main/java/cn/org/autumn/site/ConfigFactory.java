package cn.org.autumn.site;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConfigFactory extends Factory {

    private static Map<Integer, List<ConfigFactory.Config>> map = null;

    public interface Config {
        @Order(DEFAULT_ORDER)
        void update(String key, String value);
    }

    public void config(String key, String value) {
        if (null == map)
            map = getOrdered(ConfigFactory.Config.class, "update");
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<ConfigFactory.Config>> k : map.entrySet()) {
                List<ConfigFactory.Config> configs = k.getValue();
                for (ConfigFactory.Config config : configs) {
                    config.update(key, value);
                }
            }
        }
    }
}
