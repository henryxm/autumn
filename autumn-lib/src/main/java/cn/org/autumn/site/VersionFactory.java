package cn.org.autumn.site;

import cn.org.autumn.cluster.VersionHandler;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VersionFactory extends Factory {
    List<VersionHandler> versionHandlers = null;

    public Map<String, Object> getVersion() {
        if (null == versionHandlers)
            versionHandlers = getOrderList(VersionHandler.class);
        Map<String, Object> map = new HashMap<>();
        if (null != versionHandlers && !versionHandlers.isEmpty()) {
            for (VersionHandler versionHandler : versionHandlers) {
                if (StringUtils.isNotBlank(versionHandler.name())) {
                    map.putIfAbsent(versionHandler.name(), versionHandler.version());
                }
            }
        }
        return map;
    }
}