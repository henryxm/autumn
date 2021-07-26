package cn.org.autumn.site;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component
public class HostFactory extends Factory {

    private static Map<Integer, List<Host>> map = null;

    public interface Host {
        @Order(DEFAULT_ORDER)
        boolean isAllowed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
    }

    public boolean isAllowed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        if (null == map)
            map = getOrdered(Host.class, "isAllowed", HttpServletRequest.class, HttpServletResponse.class);
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<Host>> k : map.entrySet()) {
                List<Host> list = k.getValue();
                for (Host host1 : list) {
                    if (host1.isAllowed(httpServletRequest, httpServletResponse))
                        return true;
                }
            }
        }
        return false;
    }
}
