package cn.org.autumn.modules.usr.service;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.OauthUsernameToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.form.LoginForm;
import cn.org.autumn.modules.usr.service.gen.UserProfileServiceGen;
import cn.org.autumn.validator.Assert;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static cn.org.autumn.utils.Uuid.uuid;

@Service
public class UserProfileService extends UserProfileServiceGen {

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    SysUserService sysUserService;

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-user-plus";
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("usr_userprofile_table_comment", "用户信息", "User profile");
        languageService.addLanguageColumnItem("usr_userprofile_column_user_id", "用户ID", "User ID");
        languageService.addLanguageColumnItem("usr_userprofile_column_sys_user_id", "系统用户ID", "System user ID");
        languageService.addLanguageColumnItem("usr_userprofile_column_uuid", "UUID", "UUID");
        languageService.addLanguageColumnItem("usr_userprofile_column_open_id", "OPENID", "OPENID");
        languageService.addLanguageColumnItem("usr_userprofile_column_union_id", "UNIONID", "UNIONID");
        languageService.addLanguageColumnItem("usr_userprofile_column_icon", "头像", "Header icon");
        languageService.addLanguageColumnItem("usr_userprofile_column_username", "用户名", "Username");
        languageService.addLanguageColumnItem("usr_userprofile_column_nickname", "用户昵称", "Nickname");
        languageService.addLanguageColumnItem("usr_userprofile_column_mobile", "手机号", "Phone number");
        languageService.addLanguageColumnItem("usr_userprofile_column_password", "密码", "Password");
        languageService.addLanguageColumnItem("usr_userprofile_column_create_time", "创建时间", "Create time");
        super.addLanguageColumnItem();
    }

    public UserProfileEntity from(SysUserEntity sysUserEntity) {
        UserProfileEntity userProfileEntity = baseMapper.getBySysUserId(sysUserEntity.getUserId());
        if (null == userProfileEntity) {
            userProfileEntity = new UserProfileEntity();
            userProfileEntity.setSysUserId(sysUserEntity.getUserId());
            userProfileEntity.setCreateTime(new Date());
            userProfileEntity.setIcon("https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png");
            userProfileEntity.setNickname(sysUserEntity.getUsername());
            userProfileEntity.setUsername(sysUserEntity.getUsername());
            userProfileEntity.setMobile(sysUserEntity.getMobile());
            userProfileEntity.setUuid(sysUserEntity.getUuid());
            userProfileEntity.setUnionId(uuid());
            userProfileEntity.setOpenId(uuid());
            insert(userProfileEntity);
        }
        return userProfileEntity;
    }

    public UserProfileEntity queryByMobile(String mobile) {
        UserProfileEntity userEntity = new UserProfileEntity();
        userEntity.setMobile(mobile);
        return baseMapper.selectOne(userEntity);
    }

    public Map<String, Object> login(LoginForm form) {
        UserProfileEntity user = queryByMobile(form.getMobile());
        Assert.isNull(user, "手机号或密码错误");

        //密码错误
        if (!user.getPassword().equals(DigestUtils.sha256Hex(form.getPassword()))) {
            throw new AException("手机号或密码错误");
        }

        //获取登录token
        UserTokenEntity tokenEntity = userTokenService.createToken(user.getUserId());

        Map<String, Object> map = new HashMap<>(2);
        map.put("token", tokenEntity.getToken());
        map.put("expire", tokenEntity.getExpireTime().getTime() - System.currentTimeMillis());

        return map;
    }

    public void login(UserProfile userProfile) {
        boolean isLogin = ShiroUtils.isLogin();
        if (!isLogin) {
            UserProfileEntity userProfileEntity = baseMapper.getByUuid(userProfile.getUuid());
            SysUserEntity sysUserEntity = null;
            if (null == userProfileEntity) {
                sysUserEntity = sysUserService.newUser(userProfileEntity.getUsername(), "P@ssw0rd");
                userProfileEntity = from(sysUserEntity);
            }
            if (null == sysUserEntity)
                sysUserEntity = sysUserService.selectById(userProfileEntity.getSysUserId());
            Subject subject = ShiroUtils.getSubject();
            OauthUsernameToken token = new OauthUsernameToken(sysUserEntity.getUsername());
            subject.login(token);
        }
    }
}
