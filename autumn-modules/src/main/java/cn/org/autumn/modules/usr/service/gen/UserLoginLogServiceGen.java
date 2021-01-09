package cn.org.autumn.modules.usr.service.gen;

import cn.org.autumn.site.InitFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.usr.service.UsrMenu;
import cn.org.autumn.modules.usr.dao.UserLoginLogDao;
import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 登录日志控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class UserLoginLogServiceGen extends ServiceImpl<UserLoginLogDao, UserLoginLogEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected UsrMenu usrMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<UserLoginLogEntity> _page = new Query<UserLoginLogEntity>(params).getPage();
        EntityWrapper<UserLoginLogEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("userId") && null !=params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if(params.containsKey("username") && null !=params.get("username") && StringUtils.isNotEmpty(params.get("username").toString())) {
            condition.put("username", params.get("username"));
        }
        if(params.containsKey("loginTime") && null !=params.get("loginTime") && StringUtils.isNotEmpty(params.get("loginTime").toString())) {
            condition.put("login_time", params.get("loginTime"));
        }
        if(params.containsKey("logoutTime") && null !=params.get("logoutTime") && StringUtils.isNotEmpty(params.get("logoutTime").toString())) {
            condition.put("logout_time", params.get("logoutTime"));
        }
        _page.setCondition(condition);
        Page<UserLoginLogEntity> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    /**
    * need implement it in the subclass.
    * @return
    */
    public int menuOrder(){
        return 0;
    }

    /**
    * need implement it in the subclass.
    * @return
    */
    public String parentMenu(){
        usrMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(UsrMenu.usr_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String ico(){
        return "fa-file-code-o";
    }

    private String order(){
        return String.valueOf(menuOrder());
    }

    public void init() {
        sysMenuService.put(getMenus());
        language.add(getLanguageItemArray());
        language.add(getLanguageItems());
        addLanguageColumnItem();
        language.add(getLanguageItemsInternal());
    }

    public String[][] getLanguageItemArray() {
        return null;
    }

    public List<String[]> getLanguageItems() {
        return null;
    }

    public void addLanguageColumnItem(){
    }

    public String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {"usr_userloginlog_table_comment", "登录日志"},
                {"usr_userloginlog_column_id", "日志ID"},
                {"usr_userloginlog_column_user_id", "用户ID"},
                {"usr_userloginlog_column_username", "用户名"},
                {"usr_userloginlog_column_login_time", "登录时间"},
                {"usr_userloginlog_column_logout_time", "登出时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Usr", "UserLoginLog");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"登录日志", "modules/usr/userloginlog", "usr:userloginlog:list,usr:userloginlog:info,usr:userloginlog:save,usr:userloginlog:update,usr:userloginlog:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "usr_userloginlog_table_comment"},
                {"查看", null, "usr:userloginlog:list,usr:userloginlog:info", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserLoginLogInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "usr:userloginlog:save", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserLoginLogSave"), menuKey, "sys_string_add"},
                {"修改", null, "usr:userloginlog:update", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserLoginLogUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "usr:userloginlog:delete", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserLoginLogDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
