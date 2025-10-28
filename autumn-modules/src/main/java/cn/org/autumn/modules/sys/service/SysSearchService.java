package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.search.SearchHandler;
import cn.org.autumn.utils.Email;
import cn.org.autumn.utils.IDCard;
import cn.org.autumn.utils.Phone;
import cn.org.autumn.utils.QQ;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysSearchService implements SearchHandler {

    @Autowired
    SysUserService sysUserService;

    @Override
    public Object search(String text) {
        if (Email.isEmail(text) || Phone.isPhone(text) || IDCard.isIdCard(text) || QQ.isQQ(text)) {
            SysUserEntity entity = sysUserService.getUser(text);
            if (null != entity) {
                try {
                    User user = new User();
                    BeanUtils.copyProperties(user, entity);
                    return user;
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
