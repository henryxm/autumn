package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class PathFactory {

    private static Map<String, PathFactory.Path> map = null;

    public interface Path {

        String get(HttpServletRequest request, HttpServletResponse response, Model model);

        default boolean isRoot(HttpServletRequest request) {
            String uri = request.getRequestURI();
            if (StringUtils.isNotEmpty(uri) && "/".equals(uri))
                return true;
            return false;
        }

        default boolean isSpm(HttpServletRequest request) {
            if (StringUtils.isNotEmpty(request.getParameter("spm")))
                return true;
            return false;
        }
    }

    public String get(HttpServletRequest request, HttpServletResponse response, Model model) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return null;
        if (null == map)
            map = applicationContext.getBeansOfType(Path.class);
        for (Map.Entry<String, Path> k : map.entrySet()) {
            Path path = k.getValue();
            String o = path.get(request, response, model);
            if (StringUtils.isEmpty(o))
                continue;
            return o;
        }
        return null;
    }
}