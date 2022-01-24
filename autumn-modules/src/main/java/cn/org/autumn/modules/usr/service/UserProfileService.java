package cn.org.autumn.modules.usr.service;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.OauthUsernameToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.dto.VisitIp;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.gen.UserProfileServiceGen;
import cn.org.autumn.utils.Uuid;
import io.lettuce.core.KeyValue;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static cn.org.autumn.utils.Uuid.uuid;

@Service
public class UserProfileService extends UserProfileServiceGen implements LoopJob.TenSecond, LoopJob.OneMinute, LoopJob.OneHour {

    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    SysUserService sysUserService;

    static Map<String, UserProfileEntity> sync = new LinkedHashMap<>();

    static Map<String, Integer> hashUser = new HashMap<>();

    Map<String, VisitIp> visitIps = new HashMap<>();

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-user-plus";
    }

    public void init() {
        super.init();
        SysUserEntity sysUserEntity = sysUserService.getByUsername(sysUserService.getAdmin());
        from(sysUserEntity, sysUserService.getPassword(), null);
    }

    public void updateLoginIp(String uuid, String ip) {
        if (StringUtils.isBlank(ip) || StringUtils.isBlank(uuid) || ip.length() > 100)
            return;
        baseMapper.updateLoginIp(uuid, ip);
    }

    public void syncVisitIp() {
        for (Map.Entry<String, VisitIp> entry : visitIps.entrySet()) {
            if (!entry.getValue().isUpdated()) {
                baseMapper.updateVisitIp(entry.getKey(), entry.getValue().getIp());
                entry.getValue().setUpdated(true);
            }
        }
    }

    public void updateVisitIp(String uuid, String ip) {
        if (StringUtils.isBlank(ip) || StringUtils.isBlank(uuid) || ip.length() > 100)
            return;
        if (visitIps.containsKey(uuid)) {
            VisitIp visitIp = visitIps.get(uuid);
            if (!visitIp.getIp().equals(ip)) {
                visitIp.setIp(ip);
                visitIp.setUpdated(false);
            }
        } else {
            visitIps.put(uuid, new VisitIp(ip));
        }
    }

    public UserProfileEntity from(SysUserEntity sysUserEntity, String password, UserProfile merge) {
        UserProfileEntity userProfileEntity = baseMapper.getByUuid(sysUserEntity.getUuid());
        if (null == userProfileEntity) {
            userProfileEntity = baseMapper.getByUsername(sysUserEntity.getUsername());
            if (null != userProfileEntity) {
                userProfileEntity.setUuid(sysUserEntity.getUuid());
                baseMapper.setUuid(sysUserEntity.getUsername(), sysUserEntity.getUuid());
            }
        }
        if (null == userProfileEntity) {
            userProfileEntity = new UserProfileEntity();
            userProfileEntity.setPassword(password);
            userProfileEntity.setCreateTime(new Date());
            userProfileEntity.setNickname(sysUserEntity.getUsername());
            userProfileEntity.setUsername(sysUserEntity.getUsername());
            userProfileEntity.setMobile(sysUserEntity.getMobile());
            userProfileEntity.setUuid(sysUserEntity.getUuid());
            userProfileEntity.setIcon("");
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
                sysUserEntity = sysUserService.newUser(userProfile.getUsername(), userProfile.getUuid(), Uuid.uuid(), null);
                userProfileEntity = from(sysUserEntity, randomPassword, userProfile);
            }
            if (null == sysUserEntity)
                sysUserEntity = sysUserService.getByUuid(userProfileEntity.getUuid());
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
        if (null != userProfileEntity && StringUtils.isNotEmpty(userProfileEntity.getUuid())) {
            sync.put(userProfileEntity.getUuid(), userProfileEntity);
        }
    }

    private boolean checkNeedUpdate(UserProfileEntity userProfileEntity) {
        Integer integer = hashUser.get(userProfileEntity.getUuid());
        if (null == integer || integer != userProfileEntity.hashCode())
            return true;
        return false;
    }

    @Override
    public void onOneMinute() {
        syncVisitIp();
    }

    @Override
    public void onOneHour() {
        if (visitIps.size() > 10000)
            visitIps.clear();
    }

    @Override
    public void onTenSecond() {
        if (null != sync && sync.size() > 0) {
            Iterator<Map.Entry<String, UserProfileEntity>> iterator = sync.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, UserProfileEntity> entity = iterator.next();
                UserProfileEntity userProfileEntity = entity.getValue();
                if (!checkNeedUpdate(userProfileEntity))
                    continue;
                try {
                    UserProfileEntity ex = getByUuid(userProfileEntity.getUuid());
                    if (null == ex || ex.hashCode() != userProfileEntity.hashCode()) {
                        if (null != ex) {
                            userProfileEntity.setNickname(ex.getNickname());
                            userProfileEntity.setMobile(ex.getMobile());
                        }
                        UserProfileEntity username = baseMapper.getByUsername(userProfileEntity.getUsername());
                        if (null != username)
                            continue;
                        insertOrUpdate(userProfileEntity);
                    }
                } catch (Exception e) {
                    log.debug("UserProfile Synchronize Error, User uuid:" + userProfileEntity.getUuid() + ", Msg:" + e.getMessage());
                }
                hashUser.put(userProfileEntity.getUuid(), userProfileEntity.hashCode());
                iterator.remove();
            }
        }
        /**
         * 清理缓存，避免过度消耗内存
         */
        if (hashUser.size() > 10000) {
            hashUser.clear();
        }
    }
}
