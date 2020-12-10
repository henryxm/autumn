package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class LoginFactory {

    private static Map<String, LoginFactory.Login> map = null;

    public interface Login {
        boolean isNeed(HttpServletRequest httpServletRequest);
    }

    /**
     * 根据host 进行登录检查
     * 默认需要登录，只要有一个不需要登录，则不需登录
     *
     * @param httpServletRequest
     * @return
     */
    public boolean isNeed(HttpServletRequest httpServletRequest) {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return false;
        if (null == map)
            map = applicationContext.getBeansOfType(LoginFactory.Login.class);
        for (Map.Entry<String, LoginFactory.Login> k : map.entrySet()) {
            LoginFactory.Login login = k.getValue();
            if (!login.isNeed(httpServletRequest))
                return false;
        }
        return true;
    }
}
