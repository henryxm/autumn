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
 * @date 2021-06
 */
public class UserProfileServiceGen extends ServiceImpl<UserProfileDao, UserProfileEntity> implements InitFactory.Init {

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
        if(params.containsKey("uuid") && null != params.get("uuid") && StringUtils.isNotEmpty(params.get("uuid").toString())) {
            condition.put("uuid", params.get("uuid"));
        }
        if(params.containsKey("openId") && null != params.get("openId") && StringUtils.isNotEmpty(params.get("openId").toString())) {
            condition.put("open_id", params.get("openId"));
        }
        if(params.containsKey("unionId") && null != params.get("unionId") && StringUtils.isNotEmpty(params.get("unionId").toString())) {
            condition.put("union_id", params.get("unionId"));
        }
        if(params.containsKey("icon") && null != params.get("icon") && StringUtils.isNotEmpty(params.get("icon").toString())) {
            condition.put("icon", params.get("icon"));
        }
        if(params.containsKey("username") && null != params.get("username") && StringUtils.isNotEmpty(params.get("username").toString())) {
            condition.put("username", params.get("username"));
        }
        if(params.containsKey("nickname") && null != params.get("nickname") && StringUtils.isNotEmpty(params.get("nickname").toString())) {
            condition.put("nickname", params.get("nickname"));
        }
        if(params.containsKey("mobile") && null != params.get("mobile") && StringUtils.isNotEmpty(params.get("mobile").toString())) {
            condition.put("mobile", params.get("mobile"));
        }
        if(params.containsKey("password") && null != params.get("password") && StringUtils.isNotEmpty(params.get("password").toString())) {
            condition.put("password", params.get("password"));
        }
        if(params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
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
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(usrMenu.getMenu());
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Usr", "UserProfile");
        return menu;
    }

    public String button(String button) {
        String menu = menu() + button;
        return menu;
    }

    public String ico(){
        return "fa-file-code-o";
    }

    protected String order(){
        return String.valueOf(menuOrder());
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    private String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {"usr_userprofile_table_comment", "用户信息"},
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

    public List<String[]> getMenuList() {
        return null;
    }

    public String[][] getMenuItems() {
        return null;
    }

    private String[][] getMenuItemsInternal() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"用户信息", "modules/usr/userprofile", "usr:userprofile:list,usr:userprofile:info,usr:userprofile:save,usr:userprofile:update,usr:userprofile:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "usr_userprofile_table_comment"},
                {"查看", null, "usr:userprofile:list,usr:userprofile:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "usr:userprofile:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "usr:userprofile:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "usr:userprofile:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
