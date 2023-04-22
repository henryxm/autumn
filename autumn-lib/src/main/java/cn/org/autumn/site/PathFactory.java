package cn.org.autumn.site;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component
public class PathFactory extends Factory {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static Map<Integer, List<Path>> map = null;

    public interface Path {

        @Order(DEFAULT_ORDER)
        String get(HttpServletRequest request, HttpServletResponse response, Model model);

        default boolean isRoot(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return StringUtils.isNotEmpty(uri) && "/".equals(uri);
        }

        default boolean isSpm(HttpServletRequest request) {
            return StringUtils.isNotEmpty(request.getParameter("spm"));
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
                    if (log.isDebugEnabled())
                        log.debug("路径访问类:{}, 值:{}", path.getClass().getName(), o);
                    return o;
                }
            }
        }
        return null;
    }
}