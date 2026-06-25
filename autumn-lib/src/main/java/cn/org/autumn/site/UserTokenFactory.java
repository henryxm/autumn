package cn.org.autumn.site;

import cn.org.autumn.config.UserTokenHandler;
import com.google.gson.Gson;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserTokenFactory extends Factory {

    @Autowired
    Gson gson;

    List<UserTokenHandler> list = null;

    public List<UserTokenHandler> getList() {
        if (null == list) {
            list = getOrderList(UserTokenHandler.class);
        }
        return list;
    }

    public String getUser(String token) {
        for (UserTokenHandler handler : getList()) {
            try {
                if (!handler.support(token))
                    continue;
                String user = handler.getUser(token);
                if (StringUtils.isNotBlank(user))
                    return user;
            } catch (Throwable e) {
                log.error("User token compute failed: {}, error: {}", token, e.getMessage());
            }
        }
        return "";
    }
}
