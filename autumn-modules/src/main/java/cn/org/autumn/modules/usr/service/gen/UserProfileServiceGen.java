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
import cn.org.autumn.modules.usr.dao.UserProfileDao;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 用户信息控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class UserProfileServiceGen extends ServiceImpl<UserProfileDao, UserProfileEntity> implements InitFactory.Init {

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
        Page<UserProfileEntity> _page = new Query<UserProfileEntity>(params).getPage();
        EntityWrapper<UserProfileEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("userId") && null !=params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if(params.containsKey("sysUserId") && null !=params.get("sysUserId") && StringUtils.isNotEmpty(params.get("sysUserId").toString())) {
            condition.put("sys_user_id", params.get("sysUserId"));
        }
        if(params.containsKey("uuid") && null !=params.get("uuid") && StringUtils.isNotEmpty(params.get("uuid").toString())) {
            condition.put("uuid", params.get("uuid"));
        }
        if(params.containsKey("openId") && null !=params.get("openId") && StringUtils.isNotEmpty(params.get("openId").toString())) {
            condition.put("open_id", params.get("openId"));
        }
        if(params.containsKey("unionId") && null !=params.get("unionId") && StringUtils.isNotEmpty(params.get("unionId").toString())) {
            condition.put("union_id", params.get("unionId"));
        }
        if(params.containsKey("icon") && null !=params.get("icon") && StringUtils.isNotEmpty(params.get("icon").toString())) {
            condition.put("icon", params.get("icon"));
        }
        if(params.containsKey("username") && null !=params.get("username") && StringUtils.isNotEmpty(params.get("username").toString())) {
            condition.put("username", params.get("username"));
        }
        if(params.containsKey("nickname") && null !=params.get("nickname") && StringUtils.isNotEmpty(params.get("nickname").toString())) {
            condition.put("nickname", params.get("nickname"));
        }
        if(params.containsKey("mobile") && null !=params.get("mobile") && StringUtils.isNotEmpty(params.get("mobile").toString())) {
            condition.put("mobile", params.get("mobile"));
        }
        if(params.containsKey("password") && null !=params.get("password") && StringUtils.isNotEmpty(params.get("password").toString())) {
            condition.put("password", params.get("password"));
        }
        if(params.containsKey("createTime") && null !=params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<UserProfileEntity> page = this.selectPage(_page, entityEntityWrapper);
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
                {"usr_userprofile_table_comment", "用户信息"},
                {"usr_userprofile_column_user_id", "用户ID"},
                {"usr_userprofile_column_sys_user_id", "系统用户ID"},
                {"usr_userprofile_column_uuid", "UUID"},
                {"usr_userprofile_column_open_id", "OPENID"},
                {"usr_userprofile_column_union_id", "UNIONID"},
                {"usr_userprofile_column_icon", "头像"},
                {"usr_userprofile_column_username", "用户名"},
                {"usr_userprofile_column_nickname", "用户昵称"},
                {"usr_userprofile_column_mobile", "手机号"},
                {"usr_userprofile_column_password", "密码"},
                {"usr_userprofile_column_create_time", "创建时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Usr", "UserProfile");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"用户信息", "modules/usr/userprofile", "usr:userprofile:list,usr:userprofile:info,usr:userprofile:save,usr:userprofile:update,usr:userprofile:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "usr_userprofile_table_comment"},
                {"查看", null, "usr:userprofile:list,usr:userprofile:info", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserProfileInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "usr:userprofile:save", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserProfileSave"), menuKey, "sys_string_add"},
                {"修改", null, "usr:userprofile:update", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserProfileUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "usr:userprofile:delete", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserProfileDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
