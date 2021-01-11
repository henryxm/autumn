package cn.org.autumn.modules.client.service.gen;

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
import cn.org.autumn.modules.client.service.ClientMenu;
import cn.org.autumn.modules.client.dao.WebAuthenticationDao;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 网站客户端控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class WebAuthenticationServiceGen extends ServiceImpl<WebAuthenticationDao, WebAuthenticationEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected ClientMenu clientMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<WebAuthenticationEntity> _page = new Query<WebAuthenticationEntity>(params).getPage();
        EntityWrapper<WebAuthenticationEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("name") && null !=params.get("name") && StringUtils.isNotEmpty(params.get("name").toString())) {
            condition.put("name", params.get("name"));
        }
        if(params.containsKey("clientId") && null !=params.get("clientId") && StringUtils.isNotEmpty(params.get("clientId").toString())) {
            condition.put("client_id", params.get("clientId"));
        }
        if(params.containsKey("clientSecret") && null !=params.get("clientSecret") && StringUtils.isNotEmpty(params.get("clientSecret").toString())) {
            condition.put("client_secret", params.get("clientSecret"));
        }
        if(params.containsKey("redirectUri") && null !=params.get("redirectUri") && StringUtils.isNotEmpty(params.get("redirectUri").toString())) {
            condition.put("redirect_uri", params.get("redirectUri"));
        }
        if(params.containsKey("authorizeUri") && null !=params.get("authorizeUri") && StringUtils.isNotEmpty(params.get("authorizeUri").toString())) {
            condition.put("authorize_uri", params.get("authorizeUri"));
        }
        if(params.containsKey("accessTokenUri") && null !=params.get("accessTokenUri") && StringUtils.isNotEmpty(params.get("accessTokenUri").toString())) {
            condition.put("access_token_uri", params.get("accessTokenUri"));
        }
        if(params.containsKey("userInfoUri") && null !=params.get("userInfoUri") && StringUtils.isNotEmpty(params.get("userInfoUri").toString())) {
            condition.put("user_info_uri", params.get("userInfoUri"));
        }
        if(params.containsKey("scope") && null !=params.get("scope") && StringUtils.isNotEmpty(params.get("scope").toString())) {
            condition.put("scope", params.get("scope"));
        }
        if(params.containsKey("state") && null !=params.get("state") && StringUtils.isNotEmpty(params.get("state").toString())) {
            condition.put("state", params.get("state"));
        }
        if(params.containsKey("description") && null !=params.get("description") && StringUtils.isNotEmpty(params.get("description").toString())) {
            condition.put("description", params.get("description"));
        }
        if(params.containsKey("createTime") && null !=params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<WebAuthenticationEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        clientMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(clientMenu.getMenu());
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
                {"client_webauthentication_table_comment", "网站客户端"},
                {"client_webauthentication_column_id", "id"},
                {"client_webauthentication_column_name", "客户端名字"},
                {"client_webauthentication_column_client_id", "客户端ID"},
                {"client_webauthentication_column_client_secret", "客户端密匙"},
                {"client_webauthentication_column_redirect_uri", "重定向地址"},
                {"client_webauthentication_column_authorize_uri", "授权码地址"},
                {"client_webauthentication_column_access_token_uri", "Token地址"},
                {"client_webauthentication_column_user_info_uri", "用户信息地址"},
                {"client_webauthentication_column_scope", "范围"},
                {"client_webauthentication_column_state", "状态"},
                {"client_webauthentication_column_description", "描述信息"},
                {"client_webauthentication_column_create_time", "创建时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Client", "WebAuthentication");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"网站客户端", "modules/client/webauthentication", "client:webauthentication:list,client:webauthentication:info,client:webauthentication:save,client:webauthentication:update,client:webauthentication:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "client_webauthentication_table_comment"},
                {"查看", null, "client:webauthentication:list,client:webauthentication:info", "2", null, order(), SysMenuService.getMenuKey("Client", "WebAuthenticationInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "client:webauthentication:save", "2", null, order(), SysMenuService.getMenuKey("Client", "WebAuthenticationSave"), menuKey, "sys_string_add"},
                {"修改", null, "client:webauthentication:update", "2", null, order(), SysMenuService.getMenuKey("Client", "WebAuthenticationUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "client:webauthentication:delete", "2", null, order(), SysMenuService.getMenuKey("Client", "WebAuthenticationDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
