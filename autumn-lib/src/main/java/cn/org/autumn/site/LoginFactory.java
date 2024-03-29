package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component
public class LoginFactory extends Factory {

    Logger log = LoggerFactory.getLogger(getClass());

    private static Map<Integer, List<Login>> map = null;

    public interface Login {
        @Order(DEFAULT_ORDER)
        boolean isNeed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
    }

    public String getUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String query = request.getQueryString();
        if (StringUtils.isNotBlank(query)) {
            query = "?" + query;
        } else
            query = "";
        url += query;
        return url;
    }

    /**
     * 根据host 进行登录检查
     * 默认需要登录，只要有一个不需要登录，则不需登录
     */
    public boolean isNeed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return false;
        if (null == map)
            map = getOrdered(Login.class, "isNeed", HttpServletRequest.class, HttpServletResponse.class);
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<Login>> k : map.entrySet()) {
                List<Login> list = k.getValue();
                for (Login login : list) {
                    try {
                        if (!login.isNeed(httpServletRequest, httpServletResponse)) {
                            if (log.isDebugEnabled())
                                log.debug("无需登录:{}, {}", getUrl(httpServletRequest), login.getClass().getTypeName());
                            return false;
                        }
                    } catch (Throwable e) {
                        log.debug("访问异常:{},{}", login.getClass().getSimpleName(), e.getMessage());
                    }
                }
            }
        }
        if (log.isDebugEnabled())
            log.debug("需要登录:{}", getUrl(httpServletRequest));
        return true;
    }
}
