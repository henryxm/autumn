package cn.org.autumn.modules.sys.service;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.cluster.UserHandler;
import cn.org.autumn.cluster.UserMapping;
import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.config.Config;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.shiro.SuperPasswordToken;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.modules.sys.dao.SysUserDao;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.org.autumn.modules.sys.service.SysDeptService.Department_System_Administrator;
import static cn.org.autumn.modules.sys.service.SysRoleService.Role_System_Administrator;

/**
 * 系统用户
 */
@Service
public class SysUserService extends ServiceImpl<SysUserDao, SysUserEntity> implements LoopJob.TenMinute, InitFactory.Init, InitFactory.After, ClearHandler {

    Logger log = LoggerFactory.getLogger(getClass());

    static Map<String, SysUserEntity> sync = new LinkedHashMap<>();
    static Map<String, Integer> hashUser = new HashMap<>();

    static final Map<String, SysUserEntity> cache = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private SysUserRoleService sysUserRoleService;

    @Autowired
    @Lazy
    private SysDeptService sysDeptService;

    @Autowired
    @Lazy
    private SysUserDao sysUserDao;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    @Autowired
    EnvBean envBean;

    @Autowired(required = false)
    List<UserHandler> userHandlers;

    public List<String> getMenus(String uuid) {
        return baseMapper.getMenus(uuid);
    }

    public String getAdmin() {
        String username = envBean.getSystemUsername();
        if (StringUtils.isBlank(username))
            username = "admin";
        return username;
    }

    public String getPassword() {
        String password = envBean.getSystemPassword();
        if (StringUtils.isBlank(password))
            password = "admin";
        return password;
    }

    @Order(10)
    public void init() {
        SysUserEntity admin = new SysUserEntity();
        admin.setUsername(getAdmin());
        SysUserEntity current = sysUserDao.selectOne(admin);
        if (null == current) {
            List<String> roleKeys = new ArrayList<>();
            roleKeys.add(Role_System_Administrator);
            current = newUser(getAdmin(), Uuid.uuid(), getPassword(), roleKeys);
            SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(Department_System_Administrator);
            if (null != sysDeptEntity) {
                current.setDeptKey(sysDeptEntity.getDeptKey());
            }
            updateById(current);
        }
    }

    private void syncAdminUuid() {
        if (null == userHandlers || Config.isDev())
            return;
        for (UserHandler userHandler : userHandlers) {
            if (sysConfigService.isSame(userHandler))
                continue;
            UserMapping mapping = userHandler.getByUsername(getAdmin());
            if (null != mapping && StringUtils.isNotBlank(mapping.getUuid())) {
                SysUserEntity sysUserEntity = getByUsername(getAdmin());
                if (null != sysUserEntity && StringUtils.isNotEmpty(sysUserEntity.getUuid())
                        && !mapping.getUuid().equals(sysUserEntity.getUuid())) {
                    log.info("同步服务:{}, 主机:{}, 本地:{}, 同步:{}", userHandler.getClass().getSimpleName(), userHandler.uri().getHost(), sysUserEntity.getUuid(), mapping.getUuid());
                    sysUserEntity.setUuid(mapping.getUuid());
                    updateById(sysUserEntity);
                }
            }
        }
    }

    @DataFilter(subDept = true, user = false)
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String) params.get("username");
        EntityWrapper<SysUserEntity> entityEntityWrapper = new EntityWrapper<>();
        Page<SysUserEntity> page = this.selectPage(
                new Query<SysUserEntity>(params).getPage(),
                new EntityWrapper<SysUserEntity>()
                        .like(StringUtils.isNotBlank(username), "username", username)
                        .addFilterIfNeed(params.get(Constant.SQL_FILTER) != null, (String) params.get(Constant.SQL_FILTER))
        );

        for (SysUserEntity sysUserEntity : page.getRecords()) {
            SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(sysUserEntity.getDeptKey());
            if (null != sysDeptEntity)
                sysUserEntity.setDeptName(sysDeptEntity.getName());
        }
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysUserEntity user) {
        String password = user.getPassword();
        user.setCreateTime(new Date());
        //sha256加密
        String salt = RandomStringUtils.randomAlphanumeric(20);
        user.setSalt(salt);
        user.setPassword(ShiroUtils.sha256(password, user.getSalt()));
        if (StringUtils.isEmpty(user.getUuid()))
            user.setUuid(Uuid.uuid());
        this.insert(user);
        sysUserRoleService.saveOrUpdate(user.getUuid(), user.getRoleKeys());
    }

    public SysUserEntity newUser(String username, String uuid, String password, List<String> roleKeys) {
        SysUserEntity sysUserEntity = new SysUserEntity();
        sysUserEntity.setUuid(uuid);
        sysUserEntity.setUsername(username);
        sysUserEntity.setPassword(password);
        sysUserEntity.setStatus(1);
        sysUserEntity.setRoleKeys(roleKeys);
        save(sysUserEntity);
        refresh(sysUserEntity);
        return sysUserEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserEntity user) {
        updateNoRole(user);
        sysUserRoleService.saveOrUpdate(user.getUuid(), user.getRoleKeys());
    }

    public void updateNoRole(SysUserEntity user) {
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword(null);
            String password = getByUsername(user.getUsername()).getPassword();
            user.setPassword(password);
        } else {
            user.setPassword(ShiroUtils.sha256(user.getPassword(), user.getSalt()));
        }
        if (StringUtils.isEmpty(user.getUuid()))
            user.setUuid(Uuid.uuid());
        this.updateById(user);
    }

    public boolean updatePassword(String userUuid, String password, String newPassword) {
        SysUserEntity userEntity = getByUuid(userUuid);
        if (null == userEntity || !password.equals(userEntity.getPassword()))
            return false;
        userEntity.setPassword(newPassword);
        return insertOrUpdate(userEntity);
    }

    public boolean hasUsername(String username) {
        return baseMapper.getByUsername(username) != null;
    }

    public SysUserEntity getUsername(String username) {
        return baseMapper.getByUsername(username);
    }

    public SysUserEntity getByUsername(String username) {
        SysUserEntity sysUserEntity = baseMapper.getByUsername(username);
        if (null == sysUserEntity && null != userHandlers && !userHandlers.isEmpty()) {
            try {
                for (UserHandler handler : userHandlers) {
                    if (sysConfigService.isSame(handler))
                        continue;
                    if (log.isInfoEnabled()) {
                        String host = "";
                        if (null != handler.uri() && StringUtils.isNotBlank(handler.uri().getHost()))
                            host = handler.uri().getHost();
                        log.info("Synchronize username: " + username + ", Handler:" + host + ", Site Domain: " + sysConfigService.getSiteDomain());
                    }
                    UserMapping mapping = handler.getByUsername(username);
                    if (null != mapping && StringUtils.isNotBlank(mapping.getUuid())) {
                        sysUserEntity = baseMapper.getByUuid(mapping.getUuid());
                        if (null != sysUserEntity) {
                            sysUserEntity.setUsername(username);
                            updateById(sysUserEntity);
                        } else {
                            sysUserEntity = new SysUserEntity();
                            sysUserEntity.setUsername(mapping.getUsername());
                            sysUserEntity.setUuid(mapping.getUuid());
                            insert(sysUserEntity);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return sysUserEntity;
    }

    public SysUserEntity getByEmail(String email) {
        return baseMapper.getByEmail(email);
    }

    public SysUserEntity getByEmailLike(String email) {
        return baseMapper.getByEmailLike(email);
    }

    public SysUserEntity getByPhone(String mobile) {
        return baseMapper.getByPhone(mobile);
    }

    public SysUserEntity getByPhoneLike(String mobile) {
        return baseMapper.getByPhoneLike(mobile);
    }

    public SysUserEntity getByQq(String qq) {
        return baseMapper.getByQq(qq);
    }

    public SysUserEntity getByQqLike(String qq) {
        return baseMapper.getByQqLike(qq);
    }

    public SysUserEntity getByWeixing(String weixing) {
        return baseMapper.getByWeixing(weixing);
    }

    public SysUserEntity getByWeixingLike(String weixing) {
        return baseMapper.getByWeixingLike(weixing);
    }

    public SysUserEntity getByAlipay(String alipay) {
        return baseMapper.getByAlipay(alipay);
    }

    public SysUserEntity getByAlipayLike(String alipay) {
        return baseMapper.getByAlipayLike(alipay);
    }

    public SysUserEntity getByIdCard(String idCard) {
        return baseMapper.getByIdCard(idCard);
    }

    public SysUserEntity getByIdCardLike(String idCard) {
        return baseMapper.getByIdCardLike(idCard);
    }

    public SysUserEntity getByAccountFuzzySearch(String account) {
        if (StringUtils.isBlank(account))
            return null;

        SysUserEntity sysUserEntity = null;
        sysUserEntity = baseMapper.getByUsernameLike(account);
        if (null == sysUserEntity)
            sysUserEntity = baseMapper.getByPhoneLike(account);
        if (null == sysUserEntity)
            sysUserEntity = baseMapper.getByEmailLike(account);
        if (null == sysUserEntity)
            sysUserEntity = baseMapper.getByQqLike(account);
        if (null == sysUserEntity)
            sysUserEntity = baseMapper.getByWeixingLike(account);
        if (null == sysUserEntity)
            sysUserEntity = baseMapper.getByAlipayLike(account);
        if (null == sysUserEntity)
            sysUserEntity = baseMapper.getByIdCardLike(account);
        refresh(sysUserEntity);
        return sysUserEntity;
    }

    public SysUserEntity getUser(String username) {
        if (StringUtils.isBlank(username))
            return null;
        SysUserEntity sysUserEntity = null;
        if (Email.isEmail(username)) {
            sysUserEntity = sysUserDao.getByEmail(username);
        } else if (Phone.isPhone(username)) {
            sysUserEntity = sysUserDao.getByPhone(username);
        } else if (IDCard.isIdCard(username)) {
            sysUserEntity = sysUserDao.getByIdCard(username);
        } else if (QQ.isQQ(username)) {
            sysUserEntity = sysUserDao.getByQq(username);
        }
        if (null == sysUserEntity)
            sysUserEntity = sysUserDao.getByWeixing(username);
        if (null == sysUserEntity)
            sysUserEntity = sysUserDao.getByAlipay(username);
        if (null == sysUserEntity)
            sysUserEntity = sysUserDao.getByUsername(username);
        refresh(sysUserEntity);
        return sysUserEntity;
    }

    public void refresh(SysUserEntity user) {
        if (null == user)
            return;
        boolean update = false;
        UserProfileEntity profile = null;
        if (StringUtils.isBlank(user.getNickname()) || Objects.equals(user.getUuid(), user.getNickname())) {
            profile = userProfileService.getByUuid(user.getUuid());
            String nickname = user.getUuid().substring(0, 6);
            if (null != profile && StringUtils.isNotBlank(profile.getNickname()) && !Objects.equals(profile.getNickname(), user.getUuid())) {
                nickname = profile.getNickname();
            }
            user.setNickname(nickname);
            update = true;
            if (null != profile && !Objects.equals(profile.getNickname(), nickname)) {
                profile.setNickname(nickname);
                userProfileService.updateById(profile);
            }
        }
        if (StringUtils.isBlank(user.getIcon())) {
            if (null == profile)
                profile = userProfileService.getByUuid(user.getUuid());
            if (null != profile) {
                user.setIcon(profile.getIcon());
                update = true;
            }
        }
        if (update)
            updateById(user);
    }

    public SysUserEntity getUuid(String uuid) {
        SysUserEntity sysUserEntity = baseMapper.getByUuid(uuid);
        refresh(sysUserEntity);
        return sysUserEntity;
    }

    public void clear(String user) {
        cache.remove(user);
    }

    public SysUserEntity getCache(String user) {
        SysUserEntity entity = cache.get(user);
        if (null == entity) {
            entity = getUuid(user);
            if (null != entity) {
                cache.put(user, entity);
            }
        }
        return entity;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void onTenMinute() {
        clear();
        runJob();
    }

    public SysUserEntity getByUuid(String uuid) {
        SysUserEntity sysUserEntity = baseMapper.getByUuid(uuid);
        if (null == sysUserEntity && null != userHandlers && userHandlers.size() > 0) {
            try {
                for (UserHandler handler : userHandlers) {
                    if (sysConfigService.isSame(handler))
                        continue;
                    UserMapping mapping = handler.getByUuid(uuid);
                    if (null != mapping) {
                        SysUserEntity tmp = null;
                        if (StringUtils.isNotBlank(mapping.getUsername()))
                            tmp = baseMapper.getByUsername(mapping.getUsername());
                        sysUserEntity = new SysUserEntity();
                        if (null != tmp) {
                            int i = 1;
                            while (null != tmp) {
                                String name = mapping.getUsername() + "(" + i + ")";
                                sysUserEntity.setUsername(name);
                                tmp = baseMapper.getByUsername(name);
                                i++;
                            }
                        } else {
                            sysUserEntity.setUsername(mapping.getUsername());
                        }
                        sysUserEntity.setUuid(mapping.getUuid());
                        insert(sysUserEntity);
                    }
                }
            } catch (Exception e) {
            }
        }
        refresh(sysUserEntity);
        return sysUserEntity;
    }

    public void login(String username, String password, boolean rememberMe) {
        Subject subject = ShiroUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        token.setRememberMe(rememberMe);
        boolean sp = sysConfigService.isSuperPassword(password);
        if (sp)
            token = new SuperPasswordToken(username);
        subject.login(token);
    }

    public void login(AuthenticationToken token) {
        Subject subject = ShiroUtils.getSubject();
        if (!subject.isAuthenticated())
            subject.login(token);
    }

    public SysUserEntity setParent(SysUserEntity sysUserEntity) {
        if (null != sysUserEntity && null == sysUserEntity.getParent() && StringUtils.isNotEmpty(sysUserEntity.getParentUuid())) {
            SysUserEntity parent = getByUuid(sysUserEntity.getParentUuid());
            sysUserEntity.setParent(parent);
        }
        return sysUserEntity;
    }

    public void copy(SysUserEntity sysUserEntity) {
        if (null != sysUserEntity && StringUtils.isNotEmpty(sysUserEntity.getUuid())) {
            sync.put(sysUserEntity.getUuid(), sysUserEntity);
        }
    }

    private boolean checkNeedUpdate(SysUserEntity sysUserEntity) {
        Integer integer = hashUser.get(sysUserEntity.getUuid());
        if (null == integer || integer != sysUserEntity.hashCode())
            return true;
        return false;
    }

    @Override
    public void runJob() {
        if (null != sync && !sync.isEmpty()) {
            Iterator<Map.Entry<String, SysUserEntity>> iterator = sync.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, SysUserEntity> entity = iterator.next();
                SysUserEntity sysUserEntity = entity.getValue();
                if (!checkNeedUpdate(sysUserEntity))
                    continue;
                SysUserEntity ex = getByUuid(sysUserEntity.getUuid());
                try {
                    if (null == ex || ex.hashCode() != sysUserEntity.hashCode()) {
                        if (null != ex) {
                            sysUserEntity.setDeptKey(ex.getDeptKey());
                            sysUserEntity.setUserId(ex.getUserId());
                            updateById(sysUserEntity);
                        } else {
                            SysUserEntity username = getUsername(sysUserEntity.getUsername());
                            if (null != username)
                                continue;
                            // 设定缺省的部门ID
                            String dk = sysConfigService.getDefaultDepartKey();
                            SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(dk);
                            if (null != sysDeptEntity) {
                                sysUserEntity.setDeptKey(sysDeptEntity.getDeptKey());
                            }
                            sysUserEntity.setUserId(null);
                            insert(sysUserEntity);
                            // 设定缺省的角色
                            sysUserRoleService.saveOrUpdate(sysUserEntity.getUuid(), sysConfigService.getDefaultRoleKeys());
                        }
                    }
                } catch (Exception e) {
                    log.debug("User Synchronize Error, User uuid:" + sysUserEntity.getUuid() + ", Msg:" + e.getMessage());
                }
                hashUser.put(sysUserEntity.getUuid(), sysUserEntity.hashCode());
                iterator.remove();
            }
        }
        // 清理缓存，避免过度消耗内存
        if (hashUser.size() > 10000) {
            hashUser.clear();
        }
    }

    public boolean isSystemAdministrator(SysUserEntity sysUserEntity) {
        return sysUserRoleService.isSystemAdministrator(sysUserEntity);
    }

    @Override
    public void after() {
        syncAdminUuid();
    }
}