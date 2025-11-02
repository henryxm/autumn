package cn.org.autumn.site;

import cn.org.autumn.config.AccountHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AccountFactory extends Factory {

    List<AccountHandler> list = null;

    /**
     * 创建用户前，判断是否可以创建该用户，注销后的账号，系统可能不允许立即重新注册
     *
     * @param user 输入用户
     * @throws Exception 不符合注册条件的用户，抛出异常，系统将不支持注册该账号
     */
    public void creating(AccountHandler.User user) throws Exception {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.creating(user);
        }
    }

    /**
     * 监听账号注册成功时间
     *
     * @param user 注册成功的用户
     */
    public void created(AccountHandler.User user) {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.created(user);
        }
    }

    /**
     * @param user 用户
     * @throws Exception 如果不符合注销账号条件，抛出异常，系统将不会注销账号
     */
    public void canceling(AccountHandler.User user) throws Exception {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.canceling(user);
        }
    }

    /**
     * 监听账号被注销，执行清理流程
     *
     * @param user 被注销的用户
     */
    public void canceled(AccountHandler.User user) {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.canceled(user);
        }
    }

    /**
     * 正在删除用户，如果抛出异常，将不执行删除
     *
     * @param user 用户
     * @throws Exception 如果检查到异常，将不执行删除功能
     */
    public void removing(AccountHandler.User user) throws Exception {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.removing(user);
        }
    }

    /**
     * 物理删除用户成功，执行数据清理
     *
     * @param user 用户
     */
    public void removed(AccountHandler.User user) {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.removed(user);
        }
    }

    public void changing(AccountHandler.User user) throws Exception {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.changing(user);
        }
    }

    public void changed(AccountHandler.User user)   {
        if (null == user)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.changed(user);
        }
    }
}
