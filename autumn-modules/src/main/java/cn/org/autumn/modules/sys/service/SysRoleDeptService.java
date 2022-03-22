package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.modules.sys.dao.SysRoleDeptDao;
import cn.org.autumn.modules.sys.entity.SysRoleDeptEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色与部门对应关系
 */
@Service
public class SysRoleDeptService extends ServiceImpl<SysRoleDeptDao, SysRoleDeptEntity> implements InitFactory.Init {

    @Autowired
    @Lazy
    SysRoleService sysRoleService;

    @Autowired
    @Lazy
    SysDeptService sysDeptService;

    public void init() {
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(String roleKey, List<String> deptKeys) {
        //先删除角色与部门关系
        deleteBatch(new String[]{roleKey});

        if (deptKeys.size() == 0) {
            return;
        }

        SysRoleEntity sysRoleEntity = sysRoleService.getByRoleKey(roleKey);
        if (null == sysRoleEntity)
            return;

        //保存角色与菜单关系
        List<SysRoleDeptEntity> list = new ArrayList<>();
        for (String deptKey : deptKeys) {
            SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(deptKey);
            if (null == sysDeptEntity)
                continue;
            SysRoleDeptEntity sysRoleDeptEntity = new SysRoleDeptEntity();
            sysRoleDeptEntity.setDeptKey(sysDeptEntity.getDeptKey());
            sysRoleDeptEntity.setRoleKey(sysRoleEntity.getRoleKey());
            list.add(sysRoleDeptEntity);
        }
        if (!list.isEmpty())
            this.insertBatch(list);
    }

    public List<String> getDeptKeys(String[] roleKeys) {
        return baseMapper.getDeptKeys(roleKeys);
    }

    public int deleteBatch(String[] roleKeys) {
        return baseMapper.deleteByRoleKey(roleKeys);
    }
}
