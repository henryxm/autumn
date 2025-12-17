package cn.org.autumn.site;

import cn.org.autumn.config.RoleHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RoleFactory extends Factory {

    List<RoleHandler> list = null;

    public boolean isAdmin(String user) {
        if (null == list)
            list = getOrderList(RoleHandler.class);
        for (RoleHandler handler : list) {
            if (handler.isAdmin(user))
                return true;
        }
        return false;
    }
}