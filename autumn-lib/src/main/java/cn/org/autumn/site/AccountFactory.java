package cn.org.autumn.site;

import cn.org.autumn.config.AccountHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AccountFactory extends Factory {

    List<AccountHandler> list = null;

    public void creating(AccountHandler.User user) throws Exception {
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.creating(user);
        }
    }

    public void created(AccountHandler.User user) throws Exception {
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.created(user);
        }
    }

    public void canceling(AccountHandler.User user) throws Exception {
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.canceling(user);
        }
    }

    public void canceled(AccountHandler.User user) throws Exception {
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.canceled(user);
        }
    }
}
