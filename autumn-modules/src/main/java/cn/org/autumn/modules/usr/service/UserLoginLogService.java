package cn.org.autumn.modules.usr.service;

import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.gen.UserLoginLogServiceGen;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserLoginLogService extends UserLoginLogServiceGen {

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-sun-o";
    }

    public void login(String username) {
        UserLoginLogEntity userLoginLogEntity = new UserLoginLogEntity();
        userLoginLogEntity.setUsername(username);
        userLoginLogEntity.setLoginTime(new Date());
        insert(userLoginLogEntity);
    }

    public void login(UserProfileEntity userProfileEntity) {
        login(userProfileEntity.getUsername());
    }

    public void logout(String username) {
        UserLoginLogEntity userLoginLogEntity = new UserLoginLogEntity();
        userLoginLogEntity.setUsername(username);
        userLoginLogEntity.setLogoutTime(new Date());
        insert(userLoginLogEntity);
    }
}
