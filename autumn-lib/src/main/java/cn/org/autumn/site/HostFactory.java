package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class HostFactory {

    private static Map<String, HostFactory.Host> map = null;

    public interface Host {
        boolean isAllowed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
    }

    public boolean isAllowed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return false;
        if (null == map)
            map = applicationContext.getBeansOfType(HostFactory.Host.class);
        for (Map.Entry<String, HostFactory.Host> k : map.entrySet()) {
            Host host1 = k.getValue();
            if (host1.isAllowed(httpServletRequest, httpServletResponse))
                return true;
        }
        return false;
    }
}
