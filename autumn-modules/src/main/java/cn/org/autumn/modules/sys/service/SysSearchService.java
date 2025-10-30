package cn.org.autumn.modules.sys.service;

import cn.org.autumn.model.DefaultPages;
import cn.org.autumn.model.SearchTypeValue;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.search.IType;
import cn.org.autumn.search.SearchHandler;
import cn.org.autumn.utils.Email;
import cn.org.autumn.utils.IDCard;
import cn.org.autumn.utils.Phone;
import cn.org.autumn.utils.QQ;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysSearchService implements SearchHandler {

    @Autowired
    SysUserService sysUserService;

    @Override
    public List<IType> types() {
        return SearchTypeValue.of(User.class);
    }

    @Override
    public Object search(List<String> types, String text) {
        if (can(types, User.class)) {
            if (QQ.isQQ(text) || Email.isEmail(text) || Phone.isPhone(text) || IDCard.isIdCard(text)) {
                SysUserEntity entity = sysUserService.getUser(text);
                if (null != entity) {
                    try {
                        User user = new User();
                        BeanUtils.copyProperties(user, entity);
                        return DefaultPages.single(user);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }
}
