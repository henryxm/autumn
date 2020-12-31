package cn.org.autumn.modules.usr.service;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.OauthUsernameToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.form.LoginForm;
import cn.org.autumn.modules.usr.service.gen.UserProfileServiceGen;
import cn.org.autumn.utils.Uuid;
import cn.org.autumn.validator.Assert;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static cn.org.autumn.modules.sys.service.SysUserService.ADMIN;
import static cn.org.autumn.modules.sys.service.SysUserService.PASSWORD;
import static cn.org.autumn.utils.Uuid.uuid;

@Service
public class UserProfileService extends UserProfileServiceGen implements LoopJob.Job {

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    SysUserService sysUserService;

    static Map<String, UserProfileEntity> sync = new HashMap<>();

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-user-plus";
    }

    @PostConstruct
    public void init() {
        super.init();
        SysUserEntity sysUserEntity = sysUserService.getByUsername(ADMIN);
        from(sysUserEntity, PASSWORD, null);
        LoopJob.onOneMinute(this);
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

    public UserProfileEntity from(SysUserEntity sysUserEntity, String password, UserProfile merge) {
        UserProfileEntity userProfileEntity = baseMapper.getBySysUserId(sysUserEntity.getUserId());
        if (null == userProfileEntity) {
            userProfileEntity = new UserProfileEntity();
            userProfileEntity.setPassword(password);
            userProfileEntity.setSysUserId(sysUserEntity.getUserId());
            userProfileEntity.setCreateTime(new Date());
            userProfileEntity.setNickname(sysUserEntity.getUsername());
            userProfileEntity.setUsername(sysUserEntity.getUsername());
            userProfileEntity.setMobile(sysUserEntity.getMobile());
            userProfileEntity.setUuid(sysUserEntity.getUuid());
            if (null != merge) {
                userProfileEntity.setUnionId(merge.getUnionId());
                userProfileEntity.setOpenId(merge.getOpenId());
            } else {
                userProfileEntity.setUnionId(uuid());
                userProfileEntity.setOpenId(uuid());
            }
            insert(userProfileEntity);
        } else {
            boolean u = false;
            if ((StringUtils.isEmpty(userProfileEntity.getUuid()) && StringUtils.isNotEmpty(sysUserEntity.getUuid()))) {
                userProfileEntity.setUuid(sysUserEntity.getUuid());
                u = true;
            }
            if ((StringUtils.isNotEmpty(userProfileEntity.getUuid()) && StringUtils.isNotEmpty(sysUserEntity.getUuid())) && !userProfileEntity.getUuid().equals(sysUserEntity.getUuid())) {
                userProfileEntity.setUuid(sysUserEntity.getUuid());
                u = true;
            }
            if (StringUtils.isNotEmpty(password) && !password.equalsIgnoreCase(userProfileEntity.getPassword())) {
                userProfileEntity.setPassword(password);
                u = true;
            }
            if (u)
                updateById(userProfileEntity);
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
            SysUserEntity sysUserEntity = null;
            if (StringUtils.isNotEmpty(userProfile.getUsername()) && "admin".equalsIgnoreCase(userProfile.getUsername())) {
                sysUserEntity = sysUserService.getByUsername("admin");
                if (null != sysUserEntity) {
                    if (userProfile.getUuid().equalsIgnoreCase(sysUserEntity.getUuid())) {

                        UserProfileEntity userProfileEntity = baseMapper.getByUuid(sysUserEntity.getUuid());
                        if (null != userProfileEntity) {
                            userProfileEntity.setUuid(userProfile.getUuid());
                            userProfileEntity.setUnionId(userProfile.getUnionId());
                            userProfileEntity.setOpenId(userProfile.getOpenId());
                            insertOrUpdate(userProfileEntity);
                        }
                        sysUserEntity.setUuid(userProfile.getUuid());
                        sysUserService.insertOrUpdate(sysUserEntity);
                    }
                    sysUserService.login(new OauthUsernameToken(sysUserEntity.getUuid()));
                    return;
                }
            }

            UserProfileEntity userProfileEntity = baseMapper.getByUuid(userProfile.getUuid());
            if (null == userProfileEntity) {
                String randomPassword = Uuid.uuid();
                sysUserEntity = sysUserService.newUser(userProfile.getUsername(), userProfile.getUuid(), Uuid.uuid());
                userProfileEntity = from(sysUserEntity, randomPassword, userProfile);
            }
            if (null == sysUserEntity)
                sysUserEntity = sysUserService.selectById(userProfileEntity.getSysUserId());
            sysUserService.login(new OauthUsernameToken(sysUserEntity.getUuid()));
        }
    }

    public UserProfileEntity getByUuid(String uuid) {
        return baseMapper.getByUuid(uuid);
    }

    public UserProfileEntity getByOpenId(String openId) {
        return baseMapper.getByOpenId(openId);
    }

    public UserProfileEntity getByUnionId(String unionId) {
        return baseMapper.getByUnionId(unionId);
    }

    public SysUserEntity setProfile(SysUserEntity sysUserEntity) {
        if (null != sysUserEntity) {
            sysUserEntity.setProfile(getByUuid(sysUserEntity.getUuid()));
        }
        return sysUserEntity;
    }

    public void copy(UserProfileEntity userProfileEntity) {
        if (null != userProfileEntity && StringUtils.isNotEmpty(userProfileEntity.getUuid()))
            sync.put(userProfileEntity.getUuid(), userProfileEntity);
    }

    @Override
    public void runJob() {
        if (null != sync && sync.size() > 0) {
            Iterator<Map.Entry<String, UserProfileEntity>> iterator = sync.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, UserProfileEntity> entity = iterator.next();
                UserProfileEntity userProfileEntity = entity.getValue();
                UserProfileEntity ex = getByUuid(userProfileEntity.getUuid());
                if (null == ex || ex.hashCode() != userProfileEntity.hashCode()) {
                    if (null != ex) {
                        userProfileEntity.setNickname(ex.getNickname());
                        userProfileEntity.setMobile(ex.getMobile());
                    }
                    insertOrUpdate(userProfileEntity);
                }
                iterator.remove();
            }
        }
    }
}
