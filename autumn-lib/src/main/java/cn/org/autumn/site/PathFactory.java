package cn.org.autumn.site;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component
public class PathFactory extends Factory {

    private static Map<Integer, List<Path>> map = null;

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
        if (null == map)
            map = getOrdered(Path.class, "get", HttpServletRequest.class, HttpServletResponse.class, Model.class);
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<Path>> k : map.entrySet()) {
                List<Path> paths = k.getValue();
                for (Path path : paths) {
                    String o = path.get(request, response, model);
                    if (StringUtils.isEmpty(o))
                        continue;
                    return o;
                }
            }
        }
        return null;
    }
}