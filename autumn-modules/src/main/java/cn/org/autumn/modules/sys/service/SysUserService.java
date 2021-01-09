package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.shiro.SuperPasswordToken;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.sys.dao.SysUserDao;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static cn.org.autumn.modules.sys.service.SysDeptService.Department_System_Administrator;
import static cn.org.autumn.modules.sys.service.SysRoleService.Role_System_Administrator;

/**
 * 系统用户
 */
@Service
public class SysUserService extends ServiceImpl<SysUserDao, SysUserEntity> implements LoopJob.Job, InitFactory.Init {

    public static String ADMIN = "admin";
    public static String PASSWORD = "admin";

    static Map<String, SysUserEntity> sync = new LinkedHashMap<>();
    static Map<String, Integer> hashUser = new HashMap<>();

    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private SysConfigService sysConfigService;

    public List<Long> queryAllMenuId(Long userId) {
        return baseMapper.queryAllMenuId(userId);
    }

    @Order(10)
    public void init() {
        SysUserEntity admin = new SysUserEntity();
        admin.setUsername(ADMIN);
        SysUserEntity current = sysUserDao.selectOne(admin);
        if (null == current) {
            List<String> roleKeys = new ArrayList<>();
            roleKeys.add(Role_System_Administrator);
            current = newUser(ADMIN, Uuid.uuid(), PASSWORD, roleKeys);
            SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(Department_System_Administrator);
            if (null != sysDeptEntity) {
                current.setDeptId(sysDeptEntity.getDeptId());
                current.setDeptKey(sysDeptEntity.getDeptKey());
            }
            updateById(current);
        }
        LoopJob.onTenSecond(this);
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
            SysDeptEntity sysDeptEntity = sysDeptService.selectById(sysUserEntity.getDeptId());
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
        //保存用户与角色关系
        if (null != user.getRoleKeys() && user.getRoleKeys().size() > 0)
            sysUserRoleService.saveOrUpdate(user.getUuid(), user.getRoleKeys());
        else {
            sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIdList());
        }
    }

    public SysUserEntity newUser(String username, String uuid, String password, List<String> roleKeys) {
        SysUserEntity sysUserEntity = new SysUserEntity();
        sysUserEntity.setUuid(uuid);
        sysUserEntity.setUsername(username);
        sysUserEntity.setPassword(password);
        sysUserEntity.setStatus(1);
        sysUserEntity.setRoleKeys(roleKeys);
        save(sysUserEntity);
        return sysUserEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserEntity user) {
        updateNoRole(user);
        //保存用户与角色关系
        if (null != user.getRoleKeys() && user.getRoleKeys().size() > 0)
            sysUserRoleService.saveOrUpdate(user.getUuid(), user.getRoleKeys());
        else
            sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIdList());
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

    public boolean updatePassword(Long userId, String password, String newPassword) {
        SysUserEntity userEntity = selectById(userId);
        if (null == userEntity || !password.equals(userEntity.getPassword()))
            return false;
        userEntity.setPassword(newPassword);
        return insertOrUpdate(userEntity);
    }

    public SysUserEntity getByUsername(String username) {
        return baseMapper.getByUsername(username);
    }

    public SysUserEntity getByEmail(String email) {
        return baseMapper.getByEmail(email);
    }

    public SysUserEntity getByPhone(String mobile) {
        return baseMapper.getByPhone(mobile);
    }

    public SysUserEntity getByUuid(String uuid) {
        return baseMapper.getByUuid(uuid);
    }

    public void login(String username, String password) {
        Subject subject = ShiroUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
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
        if (null != sync && sync.size() > 0) {
            Iterator<Map.Entry<String, SysUserEntity>> iterator = sync.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, SysUserEntity> entity = iterator.next();
                SysUserEntity sysUserEntity = entity.getValue();

                if (!checkNeedUpdate(sysUserEntity))
                    continue;

                SysUserEntity ex = getByUuid(sysUserEntity.getUuid());

                if (null == ex || ex.hashCode() != sysUserEntity.hashCode()) {
                    if (null != ex) {
                        sysUserEntity.setDeptId(ex.getDeptId());
                        sysUserEntity.setDeptName(ex.getDeptName());
                    } else {
                        /**
                         * 设定缺省的部门ID
                         */
                        String dk = sysConfigService.getDefaultDepartKey();
                        SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(dk);
                        if (null != sysDeptEntity) {
                            sysUserEntity.setDeptId(sysDeptEntity.getDeptId());
                            sysUserEntity.setDeptKey(sysDeptEntity.getDeptKey());
                        }

                        /**
                         * 设定缺省的角色
                         */
                        sysUserRoleService.saveOrUpdate(sysUserEntity.getUuid(), sysConfigService.getDefaultRoleKeys());
                    }
                    insertOrUpdate(sysUserEntity);
                }
                hashUser.put(sysUserEntity.getUuid(), sysUserEntity.hashCode());
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

    public boolean isSystemAdministrator(SysUserEntity sysUserEntity) {
        return sysUserRoleService.isSystemAdministrator(sysUserEntity);
    }
}