package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class PathFactory {

    private static Map<String, PathFactory.Path> map = null;

    public interface Path {
        String get(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
    }

    public String get(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return null;
        if (null == map)
            map = applicationContext.getBeansOfType(Path.class);
        for (Map.Entry<String, Path> k : map.entrySet()) {
            Path path = k.getValue();
            String o = path.get(httpServletRequest, httpServletResponse);
            if (StringUtils.isEmpty(o))
                continue;
            return o;
        }
        return null;
    }
}
