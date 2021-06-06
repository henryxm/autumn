package cn.org.autumn.modules.sys.service;

import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.sys.dao.SysRoleDao;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static cn.org.autumn.modules.sys.service.SysDeptService.Department_System_Administrator;

@Service
public class SysRoleService extends ServiceImpl<SysRoleDao, SysRoleEntity> implements InitFactory.Init {
    @Autowired
    private SysRoleMenuService sysRoleMenuService;
    @Autowired
    private SysRoleDeptService sysRoleDeptService;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysDeptService sysDeptService;

    private static final String NULL = null;
    public static final String Role_System_Administrator = "Role:System:Administrator";

    @Order(0)
    public void init() {
        String[][] mapping = new String[][]{
                //{角色标识,角色名字,角色部门,角色备注}
                {Role_System_Administrator, "系统超级管理员", Department_System_Administrator, "系统超级管理员角色默认拥有系统一切角色功能权限，该角色随系统启动后自动初始化，不能删除"},
        };
        for (String[] map : mapping) {
            SysRoleEntity sysRoleEntity = new SysRoleEntity();
            String temp = map[0];
            if (NULL != temp)
                sysRoleEntity.setRoleKey(temp);
            SysRoleEntity entity = baseMapper.getByRoleKey(temp);
            if (null == entity) {
                temp = map[1];
                if (NULL != temp)
                    sysRoleEntity.setRoleName(temp);
                temp = map[2];
                if (NULL != temp) {
                    sysRoleEntity.setDeptKey(temp);
                }
                temp = map[3];
                if (NULL != temp)
                    sysRoleEntity.setRemark(temp);
                sysRoleEntity.setCreateTime(new Date());
                insert(sysRoleEntity);
            }
        }
    }

    public SysRoleEntity getByRoleKey(String roleKey) {
        return baseMapper.getByRoleKey(roleKey);
    }

    @DataFilter(subDept = true, user = false)
    public PageUtils queryPage(Map<String, Object> params) {
        String roleName = (String) params.get("roleName");
        EntityWrapper<SysRoleEntity> entityEntityWrapper = new EntityWrapper<>();
        Page<SysRoleEntity> page = this.selectPage(
                new Query<SysRoleEntity>(params).getPage(),
                new EntityWrapper<SysRoleEntity>()
                        .like(StringUtils.isNotBlank(roleName), "role_name", roleName)
                        .addFilterIfNeed(params.get(Constant.SQL_FILTER) != null, (String) params.get(Constant.SQL_FILTER))
        );

        for (SysRoleEntity sysRoleEntity : page.getRecords()) {
            SysDeptEntity sysDeptEntity = sysDeptService.getByDeptKey(sysRoleEntity.getDeptKey());
            if (sysDeptEntity != null) {
                sysRoleEntity.setDeptName(sysDeptEntity.getName());
            }
        }
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysRoleEntity role) {
        SysRoleEntity o = getByRoleKey(role.getRoleKey());
        if (null != o) {
            updateById(role);
        } else {
            role.setCreateTime(new Date());
            insert(role);
        }

        //保存角色与菜单关系
        sysRoleMenuService.saveOrUpdate(role.getRoleKey(), role.getMenuKeys());

        //保存角色与部门关系
        sysRoleDeptService.saveOrUpdate(role.getRoleKey(), role.getDeptKeys());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(String[] roleKeys) {
        //删除角色
        this.deleteBatchIds(Arrays.asList(roleKeys));
        //删除角色与菜单关联
        sysRoleMenuService.deleteBatch(roleKeys);
        //删除角色与部门关联
        sysRoleDeptService.deleteBatch(roleKeys);
        //删除角色与用户关联
        sysUserRoleService.deleteBatch(roleKeys);
    }
}
