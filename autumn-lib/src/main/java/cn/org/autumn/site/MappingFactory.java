package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component
public class MappingFactory extends Factory {

    Logger log = LoggerFactory.getLogger(getClass());

    private static Map<Integer, List<MappingFactory.Mapping>> map = null;

    public interface Mapping {

        @Order(DEFAULT_ORDER)
        boolean can(HttpServletRequest request, String value);

        String mapping(HttpServletRequest request, HttpServletResponse response, Model model, String value) throws Exception;
    }

    public boolean can(HttpServletRequest request, String value) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return false;
        if (null == map)
            map = getOrdered(MappingFactory.Mapping.class, "can", HttpServletRequest.class, String.class);
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<MappingFactory.Mapping>> k : map.entrySet()) {
                List<MappingFactory.Mapping> list = k.getValue();
                for (MappingFactory.Mapping mapping : list) {
                    try {
                        if (mapping.can(request, value)) {
                            return true;
                        }
                    } catch (Throwable e) {
                        log.error("Mapping Throwable:", e);
                    }
                }
            }
        }
        return false;
    }

    public String mapping(HttpServletRequest request, HttpServletResponse response, Model model, String value) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return "404";
        if (null == map)
            map = getOrdered(MappingFactory.Mapping.class, "can", HttpServletRequest.class, String.class);
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<MappingFactory.Mapping>> k : map.entrySet()) {
                List<MappingFactory.Mapping> list = k.getValue();
                for (MappingFactory.Mapping mapping : list) {
                    try {
                        if (mapping.can(request, value)) {
                            return mapping.mapping(request, response, model, value);
                        }
                    } catch (Throwable e) {
                        log.error("Mapping Throwable:", e);
                    }
                }
            }
        }
        return "404";
    }
}