package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.modules.sys.dao.SysRoleMenuDao;
import cn.org.autumn.modules.sys.entity.SysRoleMenuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色与菜单对应关系
 */
@Service
public class SysRoleMenuService extends ServiceImpl<SysRoleMenuDao, SysRoleMenuEntity> implements InitFactory.Init {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    public void init() {
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(Long roleId, List<Long> menuIdList) {
        //先删除角色与菜单关系
        deleteBatch(new Long[]{roleId});

        if (menuIdList.size() == 0) {
            return;
        }
        SysRoleEntity sysRoleEntity = sysRoleService.selectById(roleId);
        if (null == sysRoleEntity)
            return;

        //保存角色与菜单关系
        List<SysRoleMenuEntity> list = new ArrayList<>(menuIdList.size());
        for (Long menuId : menuIdList) {
            SysMenuEntity sysMenuEntity = sysMenuService.selectById(menuId);
            if (null == sysMenuEntity)
                continue;
            SysRoleMenuEntity sysRoleMenuEntity = new SysRoleMenuEntity();
            sysRoleMenuEntity.setMenuId(menuId);
            sysRoleMenuEntity.setMenuKey(sysMenuEntity.getMenuKey());
            sysRoleMenuEntity.setRoleId(roleId);
            sysRoleMenuEntity.setRoleKey(sysRoleEntity.getRoleKey());
            list.add(sysRoleMenuEntity);
        }
        this.insertBatch(list);
    }


    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(String roleKey, List<String> menuKeys) {
        //先删除角色与菜单关系
        deleteMenus(new String[]{roleKey});

        if (menuKeys.size() == 0) {
            return;
        }
        SysRoleEntity sysRoleEntity = sysRoleService.getByRoleKey(roleKey);
        if (null == sysRoleEntity)
            return;

        //保存角色与菜单关系
        List<SysRoleMenuEntity> list = new ArrayList<>(menuKeys.size());
        for (String menuKey : menuKeys) {
            SysRoleMenuEntity sysRoleMenuEntity = new SysRoleMenuEntity();
            SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(menuKey);
            if (null == sysMenuEntity)
                continue;
            sysRoleMenuEntity.setMenuId(sysMenuEntity.getMenuId());
            sysRoleMenuEntity.setMenuKey(menuKey);
            sysRoleMenuEntity.setRoleId(sysRoleEntity.getRoleId());
            sysRoleMenuEntity.setRoleKey(roleKey);
            list.add(sysRoleMenuEntity);
        }
        this.insertBatch(list);
    }

    public List<Long> queryMenuIdList(Long roleId) {
        return baseMapper.queryMenuIdList(roleId);
    }

    public int deleteBatch(Long[] roleIds) {
        return baseMapper.deleteBatch(roleIds);
    }

    public List<String> getMenuKeys(String roleKey) {
        return baseMapper.getMenuKeys(roleKey);
    }

    public int deleteMenus(String[] roleKeys) {
        return baseMapper.deleteMenus(roleKeys);
    }

}
