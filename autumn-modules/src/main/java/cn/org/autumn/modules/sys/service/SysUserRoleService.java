package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.site.InitFactory;
import com.aliyuncs.utils.StringUtils;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.MapUtils;
import cn.org.autumn.modules.sys.dao.SysUserRoleDao;
import cn.org.autumn.modules.sys.entity.SysUserRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static cn.org.autumn.modules.sys.service.SysRoleService.Role_System_Administrator;
import static cn.org.autumn.modules.sys.service.SysUserService.ADMIN;

/**
 * 用户与角色对应关系
 */
@Service
public class SysUserRoleService extends ServiceImpl<SysUserRoleDao, SysUserRoleEntity> implements InitFactory.Init {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysRoleService sysRoleService;

    public void init() {
        String[][] mapping = new String[][]{
                {ADMIN, Role_System_Administrator},
        };
        for (String[] map : mapping) {
            SysUserRoleEntity sysUserRoleEntity = new SysUserRoleEntity();
            String temp = map[0];
            String userUuid = "";
            if (null != temp) {
                SysUserEntity sysUserEntity = sysUserService.getByUsername(temp);
                if (null == sysUserEntity)
                    continue;
                sysUserRoleEntity.setUsername(sysUserEntity.getUsername());
                sysUserRoleEntity.setUserUuid(sysUserEntity.getUuid());
                sysUserRoleEntity.setUserId(sysUserEntity.getUserId());
                userUuid = sysUserEntity.getUuid();
            }
            temp = map[1];
            String roleKey = "";
            if (StringUtils.isNotEmpty(temp))
                roleKey = temp;
            else
                continue;
            boolean hasRole = hasUserRole(userUuid, roleKey);
            if (hasRole)
                continue;
            SysRoleEntity sysRoleEntity = sysRoleService.getByRoleKey(roleKey);
            sysUserRoleEntity.setRoleKey(roleKey);
            sysUserRoleEntity.setRoleId(sysRoleEntity.getRoleId());
            insert(sysUserRoleEntity);
        }
    }

    public boolean hasUserRole(String userUuid, String roleKey) {
        Integer i = baseMapper.hasUserRole(userUuid, roleKey);
        if (null != i && i > 0) {
            return true;
        }
        return false;
    }

    public void saveOrUpdate(Long userId, List<Long> roleIdList) {
        //先删除用户与角色关系
        this.deleteByMap(new MapUtils().put("user_id", userId));

        if (roleIdList == null || roleIdList.size() == 0) {
            return;
        }

        SysUserEntity sysUserEntity = sysUserService.selectById(userId);
        if (null == sysUserEntity)
            return;

        //保存用户与角色关系
        List<SysUserRoleEntity> list = new ArrayList<>(roleIdList.size());
        for (Long roleId : roleIdList) {
            SysRoleEntity sysRoleEntity = sysRoleService.selectById(roleId);
            if (null == sysRoleEntity)
                continue;
            SysUserRoleEntity sysUserRoleEntity = new SysUserRoleEntity();
            sysUserRoleEntity.setUserId(userId);
            sysUserRoleEntity.setRoleId(roleId);
            sysUserRoleEntity.setRoleKey(sysRoleEntity.getRoleKey());
            sysUserRoleEntity.setUsername(sysUserEntity.getUsername());
            sysUserRoleEntity.setUserUuid(sysUserEntity.getUuid());
            list.add(sysUserRoleEntity);
        }
        this.insertBatch(list);
    }

    public void saveOrUpdate(String userUuid, List<String> roleKeys) {
        //情况角色关系
        if (roleKeys == null || roleKeys.size() == 0) {
            this.deleteByMap(new MapUtils().put("user_uuid", userUuid));
            return;
        }
        List<String> existed = getRoleKeys(userUuid);
        List<String> needDeleted = new ArrayList<String>();
        List<String> needAdded = new ArrayList<>();

        if (null != existed && existed.size() > 0) {
            for (String e : existed) {
                if (roleKeys.contains(e)) {
                    continue;
                }
                needDeleted.add(e);
            }
            for (String e : roleKeys) {
                if (existed.contains(e))
                    continue;
                needAdded.add(e);
            }
        } else {
            needAdded = roleKeys;
        }

        if (needDeleted.size() > 0) {
            String[] d = new String[needDeleted.size()];
            d = needDeleted.toArray(d);
            deleteBatch(d);
        }

        //保存用户与角色关系
        List<SysUserRoleEntity> list = new ArrayList<>(needAdded.size());
        for (String roleKey : needAdded) {
            boolean has = hasUserRole(userUuid, roleKey);
            if (has)
                continue;
            SysUserRoleEntity sysUserRoleEntity = new SysUserRoleEntity();
            SysUserEntity sysUserEntity = sysUserService.getByUuid(userUuid);
            sysUserRoleEntity.setUserId(sysUserEntity.getUserId());
            sysUserRoleEntity.setUserUuid(userUuid);
            sysUserRoleEntity.setUsername(sysUserEntity.getUsername());
            SysRoleEntity sysRoleEntity = sysRoleService.getByRoleKey(roleKey);
            sysUserRoleEntity.setRoleId(sysRoleEntity.getRoleId());
            sysUserRoleEntity.setRoleKey(roleKey);
            list.add(sysUserRoleEntity);
        }
        this.insertBatch(list);
    }

    @Deprecated
    public List<Long> queryRoleIdList(Long userId) {
        return baseMapper.queryRoleIdList(userId);
    }

    public List<String> getRoleKeys(String userUuid) {
        return baseMapper.getRoleKeys(userUuid);
    }

    @Deprecated
    public int deleteBatch(Long[] roleIds) {
        return baseMapper.deleteBatch(roleIds);
    }

    public int deleteBatch(String[] roleKeys) {
        return baseMapper.deleteByRoleKeys(roleKeys);
    }

    public boolean isSystemAdministrator(SysUserEntity sysUserEntity) {
        List<String> roleKeys = baseMapper.getRoleKeys(sysUserEntity.getUuid());
        if (null != roleKeys && roleKeys.size() > 0) {
            return roleKeys.contains(Role_System_Administrator);
        }
        return false;
    }
}
