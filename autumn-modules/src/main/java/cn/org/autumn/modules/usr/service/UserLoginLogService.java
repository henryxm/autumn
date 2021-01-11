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

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {"usr_userloginlog_table_comment", "登录日志", "Login log"},
                {"usr_userloginlog_column_id", "日志ID", "Log id"},
                {"usr_userloginlog_column_user_id", "用户ID", "User id"},
                {"usr_userloginlog_column_username", "用户名", "Username"},
                {"usr_userloginlog_column_login_time", "登录时间", "Login time"},
                {"usr_userloginlog_column_logout_time", "登出时间", "Logout time"},
        };
        return items;
    }

    public void login(Long userId, String username) {
        UserLoginLogEntity userLoginLogEntity = new UserLoginLogEntity();
        userLoginLogEntity.setUserId(userId);
        userLoginLogEntity.setUsername(username);
        userLoginLogEntity.setLoginTime(new Date());
        insert(userLoginLogEntity);
    }

    public void login(UserProfileEntity userProfileEntity) {
        login(userProfileEntity.getUserId(), userProfileEntity.getUsername());
    }

    public void logout(Long userId, String username) {
        UserLoginLogEntity userLoginLogEntity = new UserLoginLogEntity();
        userLoginLogEntity.setUserId(userId);
        userLoginLogEntity.setUsername(username);
        userLoginLogEntity.setLogoutTime(new Date());
        insert(userLoginLogEntity);
    }
}
