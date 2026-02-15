package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.org.autumn.modules.sys.dao.SysRoleMenuDao;
import cn.org.autumn.modules.sys.entity.SysRoleMenuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    @Lazy
    private SysRoleService sysRoleService;

    @Autowired
    @Lazy
    private SysMenuService sysMenuService;

    public void init() {
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(String roleKey, List<String> menuKeys) {
        //先删除角色与菜单关系
        deleteMenus(new String[]{roleKey});

        if (null == menuKeys || menuKeys.size() == 0) {
            return;
        }
        SysRoleEntity sysRoleEntity = sysRoleService.getByRoleKey(roleKey);
        if (null == sysRoleEntity)
            return;

        //保存角色与菜单关系
        List<SysRoleMenuEntity> list = new ArrayList<>();
        for (String menuKey : menuKeys) {
            SysRoleMenuEntity sysRoleMenuEntity = new SysRoleMenuEntity();
            SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(menuKey);
            if (null == sysMenuEntity)
                continue;
            sysRoleMenuEntity.setMenuKey(menuKey);
            sysRoleMenuEntity.setRoleKey(roleKey);
            list.add(sysRoleMenuEntity);
        }
        if (!list.isEmpty())
            this.saveBatch(list);
    }

    public int deleteBatch(String[] roleKeys) {
        return baseMapper.deleteBatch(roleKeys);
    }

    public List<String> getMenuKeys(String roleKey) {
        return baseMapper.getMenuKeys(roleKey);
    }

    public int deleteMenus(String[] roleKeys) {
        return baseMapper.deleteMenus(roleKeys);
    }
}
