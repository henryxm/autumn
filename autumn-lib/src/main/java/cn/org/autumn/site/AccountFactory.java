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
     * @param account 输入用户
     * @throws Exception 不符合注册条件的用户，抛出异常，系统将不支持注册该账号
     */
    public void creating(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.creating(account);
        }
    }

    /**
     * 监听账号注册成功时间
     *
     * @param account 注册成功的用户
     */
    public void created(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.created(account);
        }
    }

    /**
     * @param account 用户
     * @throws Exception 如果不符合注销账号条件，抛出异常，系统将不会注销账号
     */
    public void canceling(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.canceling(account);
        }
    }

    /**
     * 监听账号被注销，执行清理流程
     *
     * @param account 被注销的用户
     */
    public void canceled(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.canceled(account);
        }
    }

    /**
     * 正在删除用户，如果抛出异常，将不执行删除
     *
     * @param account 用户
     * @throws Exception 如果检查到异常，将不执行删除功能
     */
    public void removing(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.removing(account);
        }
    }

    /**
     * 物理删除用户成功，执行数据清理
     *
     * @param account 用户
     */
    public void removed(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.removed(account);
        }
    }

    public void changing(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.changing(account);
        }
    }

    public void changed(AccountHandler.Account account)   {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.changed(account);
        }
    }

    public void disabling(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.disabling(account);
        }
    }

    public void disabled(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.disabled(account);
        }
    }

    public void enabling(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.enabling(account);
        }
    }

    public void enabled(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.enabled(account);
        }
    }

    public void deleting(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.deleting(account);
        }
    }

    public void deleted(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.deleted(account);
        }
    }

    public void destroying(AccountHandler.Account account) throws Exception {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.destroying(account);
        }
    }

    public void destroyed(AccountHandler.Account account) {
        if (null == account)
            return;
        if (null == list)
            list = getOrderList(AccountHandler.class);
        for (AccountHandler handler : list) {
            handler.destroyed(account);
        }
    }
}
