package cn.org.autumn.site;

import cn.org.autumn.config.PageHandler;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PageFactory extends Factory {

    Map<String, List<PageHandler>> map = new HashMap<>();

    private List<PageHandler> getList(String method) {
        List<PageHandler> list = null;
        if (map.containsKey(method)) {
            list = map.get(method);
        } else {
            list = getOrderList(PageHandler.class, method);
            map.put(method, list);
        }
        return list;
    }

    public String getValue(String method, String defaultValue) {
        List<PageHandler> list = getList(method);
        try {
            for (PageHandler pageHandler : list) {
                Method method1 = pageHandler.getClass().getMethod(method);
                Object o = method1.invoke(pageHandler);
                if (o instanceof String || o instanceof Integer) {
                    String value = String.valueOf(o);
                    if (StringUtils.isNotBlank(value))
                        return value;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public String getOauth2Login() {
        return getValue("getOauth2Login", "oauth2/login");
    }

    public String getLogin() {
        return getValue("getLogin", "login");
    }

    public String get404() {
        return getValue("get404", "404");
    }

    public int get404Status() {
        return Integer.parseInt(getValue("get404Status", "404"));
    }

    public String getError() {
        return getValue("getError", "error");
    }

    public int getErrorStatus() {
        return Integer.parseInt(getValue("getErrorStatus", "404"));
    }

    public String getHeader() {
        return getValue("getHeader", "header");
    }

    public String getIndex() {
        return getValue("getIndex", "index");
    }

    public String getMain() {
        return getValue("getMain", "main");
    }
}