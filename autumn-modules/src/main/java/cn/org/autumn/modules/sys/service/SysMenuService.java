package cn.org.autumn.modules.sys.service;

import cn.org.autumn.site.InitFactory;
import com.aliyuncs.utils.StringUtils;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.utils.MapUtils;
import cn.org.autumn.modules.sys.dao.SysMenuDao;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SysMenuService extends ServiceImpl<SysMenuDao, SysMenuEntity> implements InitFactory.Init {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    private static final String NULL = null;

    private Map<String, SysMenuEntity> cache = new HashMap<>();

    public static String getMenuKey(String namespace, String menuKey) {
        return "Menu:" + namespace + ":" + menuKey;
    }

    public static String getSystemMenuKey(String menuKey) {
        return "Menu:System:" + menuKey;
    }

    public static String getSystemManagementMenuKey() {
        return getSystemMenuKey("SystemManagement");
    }

    @Order(-1)
    public void init() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"系统管理", NULL, NULL, "0", "fa fa-cog", "999999", getSystemManagementMenuKey(), "", "sys_string_system_management"},
                {"管理员管理", "modules/sys/user", NULL, "1", "fa fa-user", "1", getSystemMenuKey("ManagerManagement"), getSystemMenuKey("SystemManagement"), "sys_string_manager_management"},
                {"角色管理", "modules/sys/role", NULL, "1", "fa fa-user-secret", "2", getSystemMenuKey("RoleManagement"), getSystemMenuKey("SystemManagement"), "sys_string_role_management"},
                {"菜单管理", "modules/sys/menu", NULL, "1", "fa fa-th-list", "3", getSystemMenuKey("MenuManagement"), getSystemMenuKey("SystemManagement"), "sys_string_menu_management"},
                {"SQL监控", "druid/sql.html", NULL, "1", "fa fa-bug", "4", getSystemMenuKey("SqlMonitor"), getSystemMenuKey("SystemManagement"), "sys_string_sql_monitor"},
                {"查看", NULL, "sys:user:list,sys:user:info", "2", NULL, "0", getSystemMenuKey("ManagerManagementInfo"), getSystemMenuKey("ManagerManagement"), "sys_string_lookup"},
                {"新增", NULL, "sys:user:save,sys:role:select", "2", NULL, "0", getSystemMenuKey("ManagerManagementSave"), getSystemMenuKey("ManagerManagement"), "sys_string_add"},
                {"修改", NULL, "sys:user:update,sys:role:select", "2", NULL, "0", getSystemMenuKey("ManagerManagementUpdate"), getSystemMenuKey("ManagerManagement"), "sys_string_change"},
                {"删除", NULL, "sys:user:delete", "2", NULL, "0", getSystemMenuKey("ManagerManagementDelete"), getSystemMenuKey("ManagerManagement"), "sys_string_delete"},
                {"查看", NULL, "sys:role:list,sys:role:info", "2", NULL, "0", getSystemMenuKey("RoleManagementInfo"), getSystemMenuKey("RoleManagement"), "sys_string_lookup"},
                {"新增", NULL, "sys:role:save,sys:menu:perms", "2", NULL, "0", getSystemMenuKey("RoleManagementSave"), getSystemMenuKey("RoleManagement"), "sys_string_add"},
                {"修改", NULL, "sys:role:update,sys:menu:perms", "2", NULL, "0", getSystemMenuKey("RoleManagementUpdate"), getSystemMenuKey("RoleManagement"), "sys_string_change"},
                {"删除", NULL, "sys:role:delete", "2", NULL, "0", getSystemMenuKey("RoleManagementDelete"), getSystemMenuKey("RoleManagement"), "sys_string_delete"},
                {"查看", NULL, "sys:menu:list,sys:menu:info", "2", NULL, "0", getSystemMenuKey("MenuManagementInfo"), getSystemMenuKey("MenuManagement"), "sys_string_lookup"},
                {"新增", NULL, "sys:menu:save,sys:menu:select", "2", NULL, "0", getSystemMenuKey("MenuManagementSave"), getSystemMenuKey("MenuManagement"), "sys_string_add"},
                {"修改", NULL, "sys:menu:update,sys:menu:select", "2", NULL, "0", getSystemMenuKey("MenuManagementUpdate"), getSystemMenuKey("MenuManagement"), "sys_string_change"},
                {"删除", NULL, "sys:menu:delete", "2", NULL, "0", getSystemMenuKey("MenuManagementDelete"), getSystemMenuKey("MenuManagement"), "sys_string_delete"},
                {"参数管理", "modules/sys/config", "sys:config:list,sys:config:info,sys:config:save,sys:config:update,sys:config:delete", "1", "fa fa-sun-o", "6", getSystemMenuKey("ConfigManagement"), getSystemMenuKey("SystemManagement"), "sys_string_config_management"},
                {"系统日志", "modules/sys/log", "sys:log:list", "1", "fa fa-file-text-o", "7", getSystemMenuKey("LogManagement"), getSystemMenuKey("SystemManagement"), "sys_string_system_log"},
                {"文件上传", "modules/oss/oss", "sys:oss:all", "1", "fa fa-file-image-o", "6", getSystemMenuKey("UploadManagement"), getSystemMenuKey("SystemManagement"), "sys_string_file_upload"},
                {"部门管理", "modules/sys/dept", NULL, "1", "fa fa-file-code-o", "1", getSystemMenuKey("DepartmentManagement"), getSystemMenuKey("SystemManagement"), "sys_string_department_management"},
                {"查看", NULL, "sys:dept:list,sys:dept:info", "2", NULL, "0", getSystemMenuKey("DepartmentManagementInfo"), getSystemMenuKey("DepartmentManagement"), "sys_string_lookup"},
                {"新增", NULL, "sys:dept:save,sys:dept:select", "2", NULL, "0", getSystemMenuKey("DepartmentManagementSave"), getSystemMenuKey("DepartmentManagement"), "sys_string_add"},
                {"修改", NULL, "sys:dept:update,sys:dept:select", "2", NULL, "0", getSystemMenuKey("DepartmentManagementUpdate"), getSystemMenuKey("DepartmentManagement"), "sys_string_change"},
                {"删除", NULL, "sys:dept:delete", "2", NULL, "0", getSystemMenuKey("DepartmentManagementDelete"), getSystemMenuKey("DepartmentManagement"), "sys_string_delete"},
                {"字典管理", "modules/sys/dict", NULL, "1", "fa fa-bookmark-o", "6", getSystemMenuKey("DictionaryManagement"), getSystemMenuKey("SystemManagement"), "sys_string_dictionary_management"},
                {"查看", NULL, "sys:dict:list,sys:dict:info", "2", NULL, "6", getSystemMenuKey("DictionaryManagementInfo"), getSystemMenuKey("DictionaryManagement"), "sys_string_lookup"},
                {"新增", NULL, "sys:dict:save", "2", NULL, "6", getSystemMenuKey("DictionaryManagementSave"), getSystemMenuKey("DictionaryManagement"), "sys_string_add"},
                {"修改", NULL, "sys:dict:update", "2", NULL, "6", getSystemMenuKey("DictionaryManagementUpdate"), getSystemMenuKey("DictionaryManagement"), "sys_string_change"},
                {"删除", NULL, "sys:dict:delete", "2", NULL, "6", getSystemMenuKey("DictionaryManagementDelete"), getSystemMenuKey("DictionaryManagement"), "sys_string_delete"},
        };
        put(menus);
    }

    public void put(String[][] menus){
        for (String[] menu : menus) {
            SysMenuEntity sysMenu = from(menu);
            SysMenuEntity entity = getByMenuKey(sysMenu.getMenuKey());
            if (null == entity) {
                put(sysMenu);
            }
        }
    }

    public SysMenuEntity getByMenuKey(String menuKey) {
        if (StringUtils.isEmpty(menuKey))
            return null;
        SysMenuEntity sysMenuEntity = cache.get(menuKey);
        if (null == sysMenuEntity) {
            sysMenuEntity = baseMapper.getByMenuKey(menuKey);
            if (null != sysMenuEntity)
                cache.put(menuKey, sysMenuEntity);
        }
        return sysMenuEntity;
    }

    public Integer put(SysMenuEntity sysMenuEntity) {
        SysMenuEntity parent = getByMenuKey(sysMenuEntity.getParentKey());
        if (null != parent)
            sysMenuEntity.setParentId(parent.getMenuId());
        else
            sysMenuEntity.setParentId(0L);
        cache.put(sysMenuEntity.getMenuKey(), sysMenuEntity);
        return baseMapper.insert(sysMenuEntity);
    }

    //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
    public SysMenuEntity from(String[] menu) {
        SysMenuEntity sysMenu = new SysMenuEntity();
        String temp = menu[0];
        if (NULL != temp)
            sysMenu.setName(temp);
        temp = menu[1];
        if (NULL != temp)
            sysMenu.setUrl(temp);
        temp = menu[2];
        if (NULL != temp)
            sysMenu.setPerms(temp);
        temp = menu[3];
        if (NULL != temp)
            sysMenu.setType(Integer.valueOf(temp));
        temp = menu[4];
        if (NULL != temp)
            sysMenu.setIcon(temp);
        temp = menu[5];
        if (NULL != temp)
            sysMenu.setOrderNum(Integer.valueOf(temp));
        temp = menu[6];
        if (StringUtils.isNotEmpty(temp))
            sysMenu.setMenuKey(temp);
        else
            sysMenu.setMenuKey("");
        temp = menu[7];
        if (StringUtils.isNotEmpty(temp))
            sysMenu.setParentKey(temp);
        else
            sysMenu.setParentKey("");
        temp = menu[8];
        if (StringUtils.isNotEmpty(temp))
            sysMenu.setLanguageName(temp);
        else
            sysMenu.setLanguageName("");
        return sysMenu;
    }

    public List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList) {
        List<SysMenuEntity> menuList = queryListParentId(parentId);
        if (menuIdList == null) {
            return menuList;
        }

        List<SysMenuEntity> userMenuList = new ArrayList<>();
        for (SysMenuEntity menu : menuList) {
            if (menuIdList.contains(menu.getMenuId())) {
                userMenuList.add(menu);
            }
        }
        return userMenuList;
    }

    public List<SysMenuEntity> queryListParentId(Long parentId) {
        return baseMapper.queryListParentId(parentId);
    }

    public List<SysMenuEntity> queryNotButtonList() {
        return baseMapper.queryNotButtonList();
    }

    public List<SysMenuEntity> getUserMenuList(Long userId) {
        //系统管理员，拥有最高权限
        if (userId == Constant.SUPER_ADMIN) {
            return getAllMenuList(null);
        }

        //用户菜单列表
        List<Long> menuIdList = sysUserService.queryAllMenuId(userId);
        return getAllMenuList(menuIdList);
    }

    public void delete(Long menuId) {
        //删除菜单
        this.deleteById(menuId);
        //删除菜单与角色关联
        sysRoleMenuService.deleteByMap(new MapUtils().put("menu_id", menuId));
    }

    /**
     * 获取所有菜单列表
     */
    private List<SysMenuEntity> getAllMenuList(List<Long> menuIdList) {
        //查询根菜单列表
        List<SysMenuEntity> menuList = queryListParentId(0L, menuIdList);
        //递归获取子菜单
        getMenuTreeList(menuList, menuIdList);

        return menuList;
    }

    /**
     * 递归
     */
    private List<SysMenuEntity> getMenuTreeList(List<SysMenuEntity> menuList, List<Long> menuIdList) {
        List<SysMenuEntity> subMenuList = new ArrayList<SysMenuEntity>();

        for (SysMenuEntity entity : menuList) {
            //目录
            if (entity.getType() == Constant.MenuType.CATALOG.getValue()) {
                entity.setList(getMenuTreeList(queryListParentId(entity.getMenuId(), menuIdList), menuIdList));
            }
            subMenuList.add(entity);
        }

        return subMenuList;
    }
}
