/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.sys.service;

import cn.org.autumn.table.TableInit;
import com.aliyuncs.utils.StringUtils;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.utils.MapUtils;
import cn.org.autumn.modules.sys.dao.SysMenuDao;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysMenuService extends ServiceImpl<SysMenuDao, SysMenuEntity> {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    @Autowired
    private SysMenuDao sysMenuDao;

    private static final String NULL = null;

    @Autowired
    private TableInit tableInit;

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        String[][] menus = new String[][]{
                {"1", "0", "系统管理", NULL, NULL, "0", "fa fa-cog", "999999", "", "sys_string_system_management"},
                {"2", "1", "管理员管理", "modules/sys/user", NULL, "1", "fa fa-user", "1", "", "sys_string_manager_management"},
                {"3", "1", "角色管理", "modules/sys/role", NULL, "1", "fa fa-user-secret", "2", "", "sys_string_role_management"},
                {"4", "1", "菜单管理", "modules/sys/menu", NULL, "1", "fa fa-th-list", "3", "", "sys_string_menu_management"},
                {"5", "1", "SQL监控", "druid/sql.html", NULL, "1", "fa fa-bug", "4", "", "sys_string_sql_monitor"},
                {"6", "1", "定时任务", "modules/job/schedulejob", NULL, "1", "fa fa-tasks", "5", "", "sys_string_job_schedule"},
                {"7", "6", "查看", NULL, "job:schedulejob:list,job:schedulejob:info", "2", NULL, "0", "", "sys_string_lookup"},
                {"8", "6", "新增", NULL, "job:schedulejob:save", "2", NULL, "0", "", "sys_string_add"},
                {"9", "6", "修改", NULL, "job:schedulejob:update", "2", NULL, "0", "", "sys_string_change"},
                {"10", "6", "删除", NULL, "job:schedulejob:delete", "2", NULL, "0", "", "sys_string_delete"},
                {"11", "6", "暂停", NULL, "job:schedulejob:pause", "2", NULL, "0", "", "sys_string_suspend"},
                {"12", "6", "恢复", NULL, "job:schedulejob:resume", "2", NULL, "0", "", "sys_string_resume"},
                {"13", "6", "立即执行", NULL, "job:schedulejob:run", "2", NULL, "0", "", "sys_string_immediate_execution"},
                {"14", "6", "日志列表", NULL, "job:schedulejob:log", "2", NULL, "0", "", "sys_string_log_list"},
                {"15", "2", "查看", NULL, "sys:user:list,sys:user:info", "2", NULL, "0", "", "sys_string_lookup"},
                {"16", "2", "新增", NULL, "sys:user:save,sys:role:select", "2", NULL, "0", "", "sys_string_add"},
                {"17", "2", "修改", NULL, "sys:user:update,sys:role:select", "2", NULL, "0", "", "sys_string_change"},
                {"18", "2", "删除", NULL, "sys:user:delete", "2", NULL, "0", "", "sys_string_delete"},
                {"19", "3", "查看", NULL, "sys:role:list,sys:role:info", "2", NULL, "0", "", "sys_string_lookup"},
                {"20", "3", "新增", NULL, "sys:role:save,sys:menu:perms", "2", NULL, "0", "", "sys_string_add"},
                {"21", "3", "修改", NULL, "sys:role:update,sys:menu:perms", "2", NULL, "0", "", "sys_string_change"},
                {"22", "3", "删除", NULL, "sys:role:delete", "2", NULL, "0", "", "sys_string_delete"},
                {"23", "4", "查看", NULL, "sys:menu:list,sys:menu:info", "2", NULL, "0", "", "sys_string_lookup"},
                {"24", "4", "新增", NULL, "sys:menu:save,sys:menu:select", "2", NULL, "0", "", "sys_string_add"},
                {"25", "4", "修改", NULL, "sys:menu:update,sys:menu:select", "2", NULL, "0", "", "sys_string_change"},
                {"26", "4", "删除", NULL, "sys:menu:delete", "2", NULL, "0", "", "sys_string_delete"},
                {"27", "1", "参数管理", "modules/sys/config", "sys:config:list,sys:config:info,sys:config:save,sys:config:update,sys:config:delete", "1", "fa fa-sun-o", "6", "", "sys_string_config_management"},
                {"28", "1", "系统日志", "modules/sys/log", "sys:log:list", "1", "fa fa-file-text-o", "7", "", "sys_string_system_log"},
                {"29", "1", "文件上传", "modules/oss/oss", "sys:oss:all", "1", "fa fa-file-image-o", "6", "", "sys_string_file_upload"},
                {"30", "1", "部门管理", "modules/sys/dept", NULL, "1", "fa fa-file-code-o", "1", "", "sys_string_department_management"},
                {"31", "30", "查看", NULL, "sys:dept:list,sys:dept:info", "2", NULL, "0", "", "sys_string_lookup"},
                {"32", "30", "新增", NULL, "sys:dept:save,sys:dept:select", "2", NULL, "0", "", "sys_string_add"},
                {"33", "30", "修改", NULL, "sys:dept:update,sys:dept:select", "2", NULL, "0", "", "sys_string_change"},
                {"34", "30", "删除", NULL, "sys:dept:delete", "2", NULL, "0", "", "sys_string_delete"},
                {"35", "1", "字典管理", "modules/sys/dict", NULL, "1", "fa fa-bookmark-o", "6", "", "sys_string_dictionary_management"},
                {"36", "35", "查看", NULL, "sys:dict:list,sys:dict:info", "2", NULL, "6", "", "sys_string_lookup"},
                {"37", "35", "新增", NULL, "sys:dict:save", "2", NULL, "6", "", "sys_string_add"},
                {"38", "35", "修改", NULL, "sys:dict:update", "2", NULL, "6", "", "sys_string_change"},
                {"39", "35", "删除", NULL, "sys:dict:delete", "2", NULL, "6", "", "sys_string_delete"},
        };

        for (String[] menu : menus) {
            SysMenuEntity sysMenu = from(menu);
            SysMenuEntity entity = sysMenuDao.selectOne(sysMenu);
            if (null == entity)
                sysMenuDao.insert(sysMenu);
        }
    }

    public SysMenuEntity get(SysMenuEntity sysMenuEntity) {
        return sysMenuDao.selectOne(sysMenuEntity);
    }

    public Integer put(SysMenuEntity sysMenuEntity) {
        return sysMenuDao.insert(sysMenuEntity);
    }

    public SysMenuEntity find(String[] menu) {
        SysMenuEntity sysMenu = new SysMenuEntity();
        String temp = menu[0];
        if (NULL != temp)
            sysMenu.setMenuId(Long.valueOf(temp));
        temp = menu[1];
        if (NULL != temp)
            sysMenu.setParentId(Long.valueOf(temp));
        temp = menu[2];
        if (NULL != temp)
            sysMenu.setName(temp);
        temp = menu[5];
        if (NULL != temp)
            sysMenu.setType(Integer.valueOf(temp));
        if (menu.length > 8) {
            temp = menu[8];
            if (StringUtils.isNotEmpty(temp))
                sysMenu.setMenuKey(temp);
        }
        if (menu.length > 9) {
            temp = menu[9];
            if (StringUtils.isNotEmpty(temp))
                sysMenu.setLanguageName(temp);
        }
        return sysMenu;
    }

    public SysMenuEntity from(String[] menu) {
        SysMenuEntity sysMenu = new SysMenuEntity();
        String temp = menu[0];
        if (NULL != temp)
            sysMenu.setMenuId(Long.valueOf(temp));
        temp = menu[1];
        if (NULL != temp)
            sysMenu.setParentId(Long.valueOf(temp));
        temp = menu[2];
        if (NULL != temp)
            sysMenu.setName(temp);
        temp = menu[3];
        if (NULL != temp)
            sysMenu.setUrl(temp);
        temp = menu[4];
        if (NULL != temp)
            sysMenu.setPerms(temp);
        temp = menu[5];
        if (NULL != temp)
            sysMenu.setType(Integer.valueOf(temp));
        temp = menu[6];
        if (NULL != temp)
            sysMenu.setIcon(temp);
        temp = menu[7];
        if (NULL != temp)
            sysMenu.setOrderNum(Integer.valueOf(temp));
        if (menu.length > 8) {
            temp = menu[8];
            if (StringUtils.isNotEmpty(temp))
                sysMenu.setMenuKey(temp);
        }
        if (menu.length > 9) {
            temp = menu[9];
            if (StringUtils.isNotEmpty(temp))
                sysMenu.setLanguageName(temp);
        }
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

    public SysMenuEntity getByMenuKey(String menuKey) {
        return baseMapper.getByMenuKey(menuKey);
    }
}
