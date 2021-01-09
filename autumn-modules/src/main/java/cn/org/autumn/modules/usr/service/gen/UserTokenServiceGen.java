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
import cn.org.autumn.modules.usr.dao.UserTokenDao;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 用户Token控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class UserTokenServiceGen extends ServiceImpl<UserTokenDao, UserTokenEntity> implements InitFactory.Init {

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
        Page<UserTokenEntity> _page = new Query<UserTokenEntity>(params).getPage();
        EntityWrapper<UserTokenEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("userId") && null !=params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if(params.containsKey("token") && null !=params.get("token") && StringUtils.isNotEmpty(params.get("token").toString())) {
            condition.put("token", params.get("token"));
        }
        if(params.containsKey("refreshToken") && null !=params.get("refreshToken") && StringUtils.isNotEmpty(params.get("refreshToken").toString())) {
            condition.put("refresh_token", params.get("refreshToken"));
        }
        if(params.containsKey("expireTime") && null !=params.get("expireTime") && StringUtils.isNotEmpty(params.get("expireTime").toString())) {
            condition.put("expire_time", params.get("expireTime"));
        }
        if(params.containsKey("updateTime") && null !=params.get("updateTime") && StringUtils.isNotEmpty(params.get("updateTime").toString())) {
            condition.put("update_time", params.get("updateTime"));
        }
        _page.setCondition(condition);
        Page<UserTokenEntity> page = this.selectPage(_page, entityEntityWrapper);
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
                {"usr_usertoken_table_comment", "用户Token"},
                {"usr_usertoken_column_id", "ID"},
                {"usr_usertoken_column_user_id", "用户ID"},
                {"usr_usertoken_column_token", "Token"},
                {"usr_usertoken_column_refresh_token", "Refresh Token"},
                {"usr_usertoken_column_expire_time", "过期时间"},
                {"usr_usertoken_column_update_time", "更新时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Usr", "UserToken");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"用户Token", "modules/usr/usertoken", "usr:usertoken:list,usr:usertoken:info,usr:usertoken:save,usr:usertoken:update,usr:usertoken:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "usr_usertoken_table_comment"},
                {"查看", null, "usr:usertoken:list,usr:usertoken:info", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserTokenInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "usr:usertoken:save", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserTokenSave"), menuKey, "sys_string_add"},
                {"修改", null, "usr:usertoken:update", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserTokenUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "usr:usertoken:delete", "2", null, order(), SysMenuService.getMenuKey("Usr", "UserTokenDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
