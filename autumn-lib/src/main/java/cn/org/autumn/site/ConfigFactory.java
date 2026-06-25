package cn.org.autumn.site;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConfigFactory extends Factory {

    private static Map<Integer, List<ConfigFactory.Config>> map = null;

    public static void clearOrderedHandlerCacheForJvmRestart() {
        map = null;
    }

    public interface Config {
        @Order(DEFAULT_ORDER)
        void update(String key, String value);
    }

    public void update(String key, String value) {
        if (null == map)
            map = getOrdered(ConfigFactory.Config.class, "update");
        if (null != map && !map.isEmpty()) {
            for (Map.Entry<Integer, List<ConfigFactory.Config>> k : map.entrySet()) {
                List<ConfigFactory.Config> configs = k.getValue();
                for (ConfigFactory.Config config : configs) {
                    try {
                        config.update(key, value);
                    } catch (Throwable t) {
                        log.error("Config update failed: {}, {}", key, value, t);
                    }
                }
            }
        }
    }
}
