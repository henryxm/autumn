package cn.org.autumn.modules.usr.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.OauthUsernameToken;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dao.UserProfileDao;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.dto.VisitIp;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.thread.FunctionQueues;
import cn.org.autumn.utils.IPUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.org.autumn.utils.Uuid.uuid;

@Service
public class UserProfileService extends ModuleService<UserProfileDao, UserProfileEntity> implements LoopJob.TenSecond, LoopJob.OneMinute {

    Logger log = LoggerFactory.getLogger(getClass());

    /** 单次函数队列任务最多处理条数，避免长时间占用全局串行槽 */
    private static final int SYNC_PROFILE_BATCH = 50;
    private static final int SYNC_VISIT_IP_BATCH = 100;

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    SysUserService sysUserService;

    static Map<String, UserProfileEntity> sync = new ConcurrentHashMap<>();

    static Map<String, Integer> hashUser = new ConcurrentHashMap<>();

    static Map<String, VisitIp> visitIps = new ConcurrentHashMap<>();

    @Override
    public String ico() {
        return "fa-user-plus";
    }

    public void init() {
        super.init();
        SysUserEntity sysUserEntity = sysUserService.getByUsername(sysUserService.getAdmin());
        from(sysUserEntity);
    }

    public void updateLoginIp(String uuid, String ip) {
        updateLoginIp(uuid, ip, "");
    }

    public void updateLoginIp(String uuid, String ip, String userAgent) {
        if (StringUtils.isBlank(ip) || StringUtils.isBlank(uuid) || ip.length() > 100)
            return;
        if (!CrudGuard.writable()) {
            return;
        }
        baseMapper.updateLoginIp(uuid, ip, userAgent);
    }

    public void syncVisitIp() {
        syncVisitIp(Integer.MAX_VALUE);
    }

    /**
     * @param limit 最多写库条数；返回是否仍有未同步项
     */
    public boolean syncVisitIp(int limit) {
        if (!CrudGuard.writable()) {
            return false;
        }
        int n = 0;
        for (Map.Entry<String, VisitIp> entry : visitIps.entrySet()) {
            if (n >= limit)
                return true;
            if (null == entry.getValue() || StringUtils.isBlank(entry.getValue().getIp()) || StringUtils.isBlank(entry.getValue().getUserAgent()))
                continue;
            if (!entry.getValue().isUpdated()) {
                baseMapper.updateVisitIp(entry.getKey(), entry.getValue().getIp(), entry.getValue().getUserAgent());
                entry.getValue().setUpdated(true);
                n++;
            }
        }
        return false;
    }

    public void updateVisitIp(String uuid, String ip) {
        updateVisitIp(uuid, ip, "");
    }

    public void updateVisitIp(String uuid, String ip, String userAgent) {
        if (StringUtils.isBlank(ip) || StringUtils.isBlank(uuid) || StringUtils.isBlank(userAgent))
            return;
        if (!IPUtils.isIp(ip) && !IPUtils.isIPV6(ip))
            return;
        if (visitIps.containsKey(uuid)) {
            VisitIp visitIp = visitIps.get(uuid);
            if (!visitIp.getIp().equals(ip)) {
                visitIp.setIp(ip);
                visitIp.setUserAgent(userAgent);
                visitIp.setUpdated(false);
            }
        } else {
            visitIps.put(uuid, new VisitIp(ip, userAgent));
        }
    }

    public UserProfileEntity from(SysUserEntity sysUserEntity) {
        if (null == sysUserEntity)
            return null;
        UserProfileEntity userProfileEntity = baseMapper.getByUuid(sysUserEntity.getUuid());
        if (null == userProfileEntity) {
            userProfileEntity = baseMapper.getByUsername(sysUserEntity.getUsername());
            if (null != userProfileEntity) {
                userProfileEntity.setUuid(sysUserEntity.getUuid());
                if (CrudGuard.writable()) {
                    baseMapper.setUuid(sysUserEntity.getUsername(), sysUserEntity.getUuid());
                }
            }
        }
        if (null == userProfileEntity) {
            userProfileEntity = new UserProfileEntity();
            userProfileEntity.setCreateTime(new Date());
            userProfileEntity.setNickname(sysUserEntity.getNickname());
            userProfileEntity.setUsername(sysUserEntity.getUsername());
            userProfileEntity.setMobile(sysUserEntity.getMobile());
            userProfileEntity.setUuid(sysUserEntity.getUuid());
            userProfileEntity.setIcon(sysUserEntity.getIcon());
            if (CrudGuard.writable()) {
                save(userProfileEntity);
            }
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
            if (u && CrudGuard.writable())
                updateById(userProfileEntity);
        }
        return userProfileEntity;
    }

    public UserProfileEntity queryByMobile(String mobile) {
        if (StringUtils.isBlank(mobile)) return null;
        return getOne(new QueryWrapper<UserProfileEntity>().eq(columnInWrapper("mobile"), mobile));
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
                            saveOrUpdate(userProfileEntity);
                        }
                        sysUserEntity.setUuid(userProfile.getUuid());
                        sysUserService.saveOrUpdate(sysUserEntity);
                    }
                    sysUserService.login(new OauthUsernameToken(sysUserEntity.getUuid()));
                    return;
                }
            }
            UserProfileEntity userProfileEntity = baseMapper.getByUuid(userProfile.getUuid());
            if (null == userProfileEntity) {
                sysUserEntity = sysUserService.getByUuid(userProfile.getUuid());
                if (sysUserEntity == null) {
                    throw new IllegalStateException("本地用户不存在，请先完成账号绑定");
                }
                userProfileEntity = from(sysUserEntity);
            }
            if (null == sysUserEntity)
                sysUserEntity = sysUserService.getByUuid(userProfileEntity.getUuid());
            sysUserService.login(new OauthUsernameToken(sysUserEntity.getUuid()));
        }
    }

    /** OAuth 绑定解析完成后建立 Session：要求本地用户已存在，不自动建号。 */
    public void establishSession(UserProfile userProfile) {
        if (userProfile == null || StringUtils.isBlank(userProfile.getUuid())) {
            throw new IllegalStateException("本地用户不存在");
        }
        if (ShiroUtils.isLogin() && userProfile.getUuid().equalsIgnoreCase(ShiroUtils.getUserUuid())) {
            return;
        }
        SysUserEntity sysUserEntity = sysUserService.getByUuid(userProfile.getUuid());
        if (sysUserEntity == null) {
            throw new IllegalStateException("本地用户不存在");
        }
        sysUserService.login(new OauthUsernameToken(sysUserEntity.getUuid()));
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
        return null == integer || integer != userProfileEntity.hashCode();
    }

    @Override
    public void onOneMinute() {
        FunctionQueues.offer("UserProfileService.syncVisitIp", this::runSyncVisitIpJob);
    }

    private void runSyncVisitIpJob() {
        try {
            boolean more = syncVisitIp(SYNC_VISIT_IP_BATCH);
            if (more) {
                FunctionQueues.offer("UserProfileService.syncVisitIp", this::runSyncVisitIpJob);
                return;
            }
            // 全部已写库后再清理，避免分批间隙丢失未写完的 IP
            visitIps.entrySet().removeIf(e -> e.getValue() != null && e.getValue().isUpdated());
        } catch (Exception e) {
            log.error("Synchronize User IP:{}", e.getMessage());
        }
    }

    @Override
    public void onTenSecond() {
        FunctionQueues.offer("UserProfileService.syncProfile", this::runSyncProfileJob);
    }

    private void runSyncProfileJob() {
        try {
            if (null == sync || sync.isEmpty()) {
                if (hashUser.size() > 10000)
                    hashUser.clear();
                return;
            }
            int processed = 0;
            Iterator<Map.Entry<String, UserProfileEntity>> iterator = sync.entrySet().iterator();
            while (iterator.hasNext() && processed < SYNC_PROFILE_BATCH) {
                Map.Entry<String, UserProfileEntity> entity = iterator.next();
                UserProfileEntity userProfileEntity = entity.getValue();
                if (!checkNeedUpdate(userProfileEntity)) {
                    iterator.remove();
                    continue;
                }
                try {
                    UserProfileEntity ex = getByUuid(userProfileEntity.getUuid());
                    if (null == ex || ex.hashCode() != userProfileEntity.hashCode()) {
                        if (null != ex) {
                            userProfileEntity.setNickname(ex.getNickname());
                            userProfileEntity.setMobile(ex.getMobile());
                        }
                        UserProfileEntity username = baseMapper.getByUsername(userProfileEntity.getUsername());
                        if (null != username) {
                            iterator.remove();
                            continue;
                        }
                        saveOrUpdate(userProfileEntity);
                    }
                } catch (Exception e) {
                    log.debug("UserProfile Synchronize Error, User uuid:" + userProfileEntity.getUuid() + ", Msg:" + e.getMessage());
                }
                hashUser.put(userProfileEntity.getUuid(), userProfileEntity.hashCode());
                iterator.remove();
                processed++;
            }
            if (hashUser.size() > 10000)
                hashUser.clear();
            // 仍有积压则再投递一批，让出串行槽给防火墙等其它任务
            if (!sync.isEmpty())
                FunctionQueues.offer("UserProfileService.syncProfile", this::runSyncProfileJob);
        } catch (Exception e) {
            log.error("Synchronize User Profile:{}", e.getMessage());
        }
    }
}
