package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.site.InitFactory;
import com.aliyuncs.utils.StringUtils;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.utils.MapUtils;
import cn.org.autumn.modules.sys.dao.SysMenuDao;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SysMenuService extends ServiceImpl<SysMenuDao, SysMenuEntity> implements InitFactory.Init {

    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private SysRoleMenuService sysRoleMenuService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    private static final String NULL = null;

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

    private Map<String, SysMenuEntity> merge(Object... objects) {
        Map<String, SysMenuEntity> map = new LinkedHashMap<>();
        if (null != objects && objects.length > 0) {
            for (Object o : objects) {
                if (null == o)
                    continue;
                if (o instanceof List) {
                    List l = (List) o;
                    if (l.isEmpty())
                        continue;
                    for (Object b : l) {
                        if (b instanceof String[]) {
                            String[] menu = (String[]) b;
                            SysMenuEntity sysMenu = from(menu);
                            map.put(sysMenu.getMenuKey(), sysMenu);
                        }
                    }
                }
                if (o instanceof String[][]) {
                    String[][] menus = (String[][]) o;
                    for (String[] menu : menus) {
                        SysMenuEntity sysMenu = from(menu);
                        map.put(sysMenu.getMenuKey(), sysMenu);
                    }
                }
            }
        }
        return map;
    }

    public void put(Object... objects) {
        try {
            boolean update = sysConfigService.isUpdateMenu();
            put(update, objects);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    public void put(boolean update, Object... objects) {
        Map<String, SysMenuEntity> menus = merge(objects);
        for (Map.Entry<String, SysMenuEntity> entry : menus.entrySet()) {
            SysMenuEntity sysMenu = entry.getValue();
            SysMenuEntity entity = getByMenuKey(sysMenu.getMenuKey());
            if (null == entity) {
                put(sysMenu);
            } else {
                if (update && (sysMenu.hashCode() != entity.hashCode() || null == entity.getParentKey())) {
                    sysMenu.setMenuKey(entity.getMenuKey());
                    put(sysMenu);
                }
            }
        }
    }

    public void put(String[][] menus) {
        for (String[] menu : menus) {
            SysMenuEntity sysMenu = from(menu);
            SysMenuEntity entity = getByMenuKey(sysMenu.getMenuKey());
            if (null == entity) {
                put(sysMenu);
            }
        }
    }

    public SysMenuEntity getByMenuKey(String menuKey) {
        return baseMapper.getByMenuKey(menuKey);
    }

    public boolean put(SysMenuEntity sysMenuEntity) {
        SysMenuEntity parent = getByMenuKey(sysMenuEntity.getParentKey());
        if (null != parent)
            sysMenuEntity.setParentKey(parent.getMenuKey());
        else
            sysMenuEntity.setParentKey("");
        SysMenuEntity current = getByMenuKey(sysMenuEntity.getMenuKey());
        if (null != current) {
            current.copy(sysMenuEntity);
        } else
            current = sysMenuEntity;
        return insertOrUpdate(current);
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

    public List<SysMenuEntity> queryListParentId(String parentKey, List<String> menuKeys) {
        List<SysMenuEntity> menuList = getByParentKey(parentKey);
        if (menuKeys == null) {
            return menuList;
        }

        List<SysMenuEntity> userMenuList = new ArrayList<>();
        for (SysMenuEntity menu : menuList) {
            if (menuKeys.contains(menu.getMenuKey())) {
                userMenuList.add(menu);
            }
        }
        return userMenuList;
    }

    public List<SysMenuEntity> getByParentKey(String parentKey) {
        return baseMapper.getByParentKey(parentKey);
    }

    public List<SysMenuEntity> queryNotButtonList() {
        return baseMapper.queryNotButtonList();
    }

    public List<SysMenuEntity> getUserMenuList(String userUuid) {
        SysUserEntity sysUserEntity = sysUserService.getByUuid(userUuid);
        return getMenus(sysUserEntity);
    }

    public List<SysMenuEntity> getMenus(SysUserEntity sysUserEntity) {
        boolean isAdmin = sysUserService.isSystemAdministrator(sysUserEntity);
        //系统管理员，拥有最高权限
        if (isAdmin) {
            return getAllMenuList(null);
        }
        //用户菜单列表
        List<String> menuIdList = sysUserService.getMenus(sysUserEntity.getUuid());
        return getAllMenuList(menuIdList);
    }

    public void delete(String menuKey) {
        //删除菜单
        this.deleteByMenuKeys(new String[]{menuKey});
        //删除菜单与角色关联
        sysRoleMenuService.deleteByMap(new MapUtils().put("menu_key", menuKey));
    }

    public int deleteByMenuKeys(String[] menuKeys) {
        return baseMapper.deleteByMenuKeys(menuKeys);
    }

    /**
     * 获取所有菜单列表
     */
    private List<SysMenuEntity> getAllMenuList(List<String> menuIdList) {
        //查询根菜单列表
        List<SysMenuEntity> menuList = queryListParentId("", menuIdList);
        //递归获取子菜单
        getMenuTreeList(menuList, menuIdList);

        return menuList;
    }

    /**
     * 递归
     */
    private List<SysMenuEntity> getMenuTreeList(List<SysMenuEntity> menuList, List<String> menuKeyList) {
        List<SysMenuEntity> subMenuList = new ArrayList<SysMenuEntity>();
        for (SysMenuEntity entity : menuList) {
            //目录
            if (entity.getType() == Constant.MenuType.CATALOG.getValue()) {
                entity.setList(getMenuTreeList(queryListParentId(entity.getMenuKey(), menuKeyList), menuKeyList));
            }
            subMenuList.add(entity);
        }
        return subMenuList;
    }
}
