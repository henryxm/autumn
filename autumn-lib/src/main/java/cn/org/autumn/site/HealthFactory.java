package cn.org.autumn.site;

import cn.org.autumn.cluster.HealthHandler;
import cn.org.autumn.config.ViewHandler;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HealthFactory extends Factory {
    List<HealthHandler> healthHandlers = null;

    public Map<String, Object> getHealth() {
        if (null == healthHandlers)
            healthHandlers = getOrderList(HealthHandler.class);
        Map<String, Object> map = new HashMap<>();
        if (null != healthHandlers && !healthHandlers.isEmpty()) {
            for (HealthHandler healthHandler : healthHandlers) {
                if (StringUtils.isNotBlank(healthHandler.name())) {
                    map.putIfAbsent(healthHandler.name(), healthHandler.value());
                }
            }
        }
        return map;
    }
}