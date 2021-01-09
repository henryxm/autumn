package cn.org.autumn.modules.oauth.service.gen;

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
import cn.org.autumn.modules.oauth.service.OauthMenu;
import cn.org.autumn.modules.oauth.dao.TokenStoreDao;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 授权令牌控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class TokenStoreServiceGen extends ServiceImpl<TokenStoreDao, TokenStoreEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected OauthMenu oauthMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<TokenStoreEntity> _page = new Query<TokenStoreEntity>(params).getPage();
        EntityWrapper<TokenStoreEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("userId") && null !=params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if(params.containsKey("userUuid") && null !=params.get("userUuid") && StringUtils.isNotEmpty(params.get("userUuid").toString())) {
            condition.put("user_uuid", params.get("userUuid"));
        }
        if(params.containsKey("authCode") && null !=params.get("authCode") && StringUtils.isNotEmpty(params.get("authCode").toString())) {
            condition.put("auth_code", params.get("authCode"));
        }
        if(params.containsKey("accessToken") && null !=params.get("accessToken") && StringUtils.isNotEmpty(params.get("accessToken").toString())) {
            condition.put("access_token", params.get("accessToken"));
        }
        if(params.containsKey("accessTokenExpiredIn") && null !=params.get("accessTokenExpiredIn") && StringUtils.isNotEmpty(params.get("accessTokenExpiredIn").toString())) {
            condition.put("access_token_expired_in", params.get("accessTokenExpiredIn"));
        }
        if(params.containsKey("refreshToken") && null !=params.get("refreshToken") && StringUtils.isNotEmpty(params.get("refreshToken").toString())) {
            condition.put("refresh_token", params.get("refreshToken"));
        }
        if(params.containsKey("refreshTokenExpiredIn") && null !=params.get("refreshTokenExpiredIn") && StringUtils.isNotEmpty(params.get("refreshTokenExpiredIn").toString())) {
            condition.put("refresh_token_expired_in", params.get("refreshTokenExpiredIn"));
        }
        if(params.containsKey("createTime") && null !=params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<TokenStoreEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        oauthMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(OauthMenu.oauth_menu);
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
                {"oauth_tokenstore_table_comment", "授权令牌"},
                {"oauth_tokenstore_column_id", "id"},
                {"oauth_tokenstore_column_user_id", "用户ID"},
                {"oauth_tokenstore_column_user_uuid", "用户Uuid"},
                {"oauth_tokenstore_column_auth_code", "授权码"},
                {"oauth_tokenstore_column_access_token", "访问令牌"},
                {"oauth_tokenstore_column_access_token_expired_in", "访问令牌有效时长(秒)"},
                {"oauth_tokenstore_column_refresh_token", "刷新令牌"},
                {"oauth_tokenstore_column_refresh_token_expired_in", "刷新令牌有效时长(秒)"},
                {"oauth_tokenstore_column_create_time", "创建时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Oauth", "TokenStore");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"授权令牌", "modules/oauth/tokenstore", "oauth:tokenstore:list,oauth:tokenstore:info,oauth:tokenstore:save,oauth:tokenstore:update,oauth:tokenstore:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "oauth_tokenstore_table_comment"},
                {"查看", null, "oauth:tokenstore:list,oauth:tokenstore:info", "2", null, order(), SysMenuService.getMenuKey("Oauth", "TokenStoreInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "oauth:tokenstore:save", "2", null, order(), SysMenuService.getMenuKey("Oauth", "TokenStoreSave"), menuKey, "sys_string_add"},
                {"修改", null, "oauth:tokenstore:update", "2", null, order(), SysMenuService.getMenuKey("Oauth", "TokenStoreUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "oauth:tokenstore:delete", "2", null, order(), SysMenuService.getMenuKey("Oauth", "TokenStoreDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
