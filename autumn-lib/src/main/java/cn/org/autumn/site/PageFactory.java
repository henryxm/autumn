package cn.org.autumn.site;

import cn.org.autumn.config.PageHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PageFactory extends Factory {
    static final Logger log = LoggerFactory.getLogger(PageFactory.class);

    @Autowired
    ClearFactory clearFactory;

    @Autowired
    TableFactory tableFactory;

    Map<String, List<PageHandler>> map = new HashMap<>();

    private List<PageHandler> getList(String method) {
        List<PageHandler> list = null;
        if (map.containsKey(method)) {
            list = map.get(method);
        } else {
            list = getOrderList(PageHandler.class, method, HttpServletRequest.class, HttpServletResponse.class, Model.class);
            map.put(method, list);
        }
        return list;
    }

    public String invoke(String method, String defaultValue, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletRequest && log.isDebugEnabled()) {
            String query = httpServletRequest.getQueryString();
            if (StringUtils.isNotBlank(query))
                query = "?" + query;
            else
                query = "";
            log.debug("{}:{}{}", method, httpServletRequest.getRequestURL().toString(), query);
        }
        List<PageHandler> list = getList(method);
        try {
            for (PageHandler pageHandler : list) {
                Method method1 = pageHandler.getClass().getMethod(method, HttpServletRequest.class, HttpServletResponse.class, Model.class);
                Object o = method1.invoke(pageHandler, httpServletRequest, httpServletResponse, model);
                if (o instanceof String || o instanceof Integer) {
                    String value = String.valueOf(o);
                    if (StringUtils.isNotBlank(value)) {
                        if (log.isDebugEnabled())
                            log.debug("跳转执行:{}", value);
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Invoke:{}", e.getMessage());
        }
        if (log.isDebugEnabled())
            log.debug("默认执行:{}", defaultValue);
        return defaultValue;
    }

    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("login", "login", httpServletRequest, httpServletResponse, model);
    }

    public String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("logout", "login", httpServletRequest, httpServletResponse, model);
    }

    public String direct(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("direct", "direct", httpServletRequest, httpServletResponse, model);
    }

    public String direct(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String url) {
        if (!model.containsAttribute("url"))
            model.addAttribute("url", url);
        return direct(httpServletRequest, httpServletResponse, model);
    }

    public String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(404);
        return invoke("_404", "404", httpServletRequest, httpServletResponse, model);
    }

    public String _500(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(500);
        return invoke("_500", "500", httpServletRequest, httpServletResponse, model);
    }

    public String _505(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(505);
        return invoke("_505", "505", httpServletRequest, httpServletResponse, model);
    }

    public String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(500);
        return invoke("error", "error", httpServletRequest, httpServletResponse, model);
    }

    public String header(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("header", "header", httpServletRequest, httpServletResponse, model);
    }

    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("index", "index", httpServletRequest, httpServletResponse, model);
    }

    public String main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("main", "main", httpServletRequest, httpServletResponse, model);
    }

    public String loading(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("loading", "loading", httpServletRequest, httpServletResponse, model);
    }

    public String clear(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        model.addAttribute("data", clearFactory.clear());
        return invoke("clear", "clear", httpServletRequest, httpServletResponse, model);
    }

    public String reinit(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        model.addAttribute("data", tableFactory.reinit());
        return invoke("reinit", "reinit", httpServletRequest, httpServletResponse, model);
    }

    public String wall(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return invoke("wall", "wall", httpServletRequest, httpServletResponse, model);
    }
}