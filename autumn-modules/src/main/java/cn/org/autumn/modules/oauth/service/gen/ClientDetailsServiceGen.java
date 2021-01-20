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
import cn.org.autumn.modules.oauth.dao.ClientDetailsDao;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 客户端详情控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class ClientDetailsServiceGen extends ServiceImpl<ClientDetailsDao, ClientDetailsEntity> implements InitFactory.Init {

    @Autowired
    protected OauthMenu oauthMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<ClientDetailsEntity> _page = new Query<ClientDetailsEntity>(params).getPage();
        EntityWrapper<ClientDetailsEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("resourceIds") && null != params.get("resourceIds") && StringUtils.isNotEmpty(params.get("resourceIds").toString())) {
            condition.put("resource_ids", params.get("resourceIds"));
        }
        if(params.containsKey("scope") && null != params.get("scope") && StringUtils.isNotEmpty(params.get("scope").toString())) {
            condition.put("scope", params.get("scope"));
        }
        if(params.containsKey("grantTypes") && null != params.get("grantTypes") && StringUtils.isNotEmpty(params.get("grantTypes").toString())) {
            condition.put("grant_types", params.get("grantTypes"));
        }
        if(params.containsKey("roles") && null != params.get("roles") && StringUtils.isNotEmpty(params.get("roles").toString())) {
            condition.put("roles", params.get("roles"));
        }
        if(params.containsKey("trusted") && null != params.get("trusted") && StringUtils.isNotEmpty(params.get("trusted").toString())) {
            condition.put("trusted", params.get("trusted"));
        }
        if(params.containsKey("archived") && null != params.get("archived") && StringUtils.isNotEmpty(params.get("archived").toString())) {
            condition.put("archived", params.get("archived"));
        }
        if(params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        if(params.containsKey("clientId") && null != params.get("clientId") && StringUtils.isNotEmpty(params.get("clientId").toString())) {
            condition.put("client_id", params.get("clientId"));
        }
        if(params.containsKey("clientSecret") && null != params.get("clientSecret") && StringUtils.isNotEmpty(params.get("clientSecret").toString())) {
            condition.put("client_secret", params.get("clientSecret"));
        }
        if(params.containsKey("clientName") && null != params.get("clientName") && StringUtils.isNotEmpty(params.get("clientName").toString())) {
            condition.put("client_name", params.get("clientName"));
        }
        if(params.containsKey("clientUri") && null != params.get("clientUri") && StringUtils.isNotEmpty(params.get("clientUri").toString())) {
            condition.put("client_uri", params.get("clientUri"));
        }
        if(params.containsKey("clientIconUri") && null != params.get("clientIconUri") && StringUtils.isNotEmpty(params.get("clientIconUri").toString())) {
            condition.put("client_icon_uri", params.get("clientIconUri"));
        }
        if(params.containsKey("redirectUri") && null != params.get("redirectUri") && StringUtils.isNotEmpty(params.get("redirectUri").toString())) {
            condition.put("redirect_uri", params.get("redirectUri"));
        }
        if(params.containsKey("description") && null != params.get("description") && StringUtils.isNotEmpty(params.get("description").toString())) {
            condition.put("description", params.get("description"));
        }
        _page.setCondition(condition);
        Page<ClientDetailsEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(oauthMenu.getMenu());
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Oauth", "ClientDetails");
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
                {"oauth_clientdetails_table_comment", "客户端详情"},
                {"oauth_clientdetails_column_id", "id"},
                {"oauth_clientdetails_column_resource_ids", "资源ID"},
                {"oauth_clientdetails_column_scope", "范围"},
                {"oauth_clientdetails_column_grant_types", "授权类型"},
                {"oauth_clientdetails_column_roles", "角色"},
                {"oauth_clientdetails_column_trusted", "是否可信"},
                {"oauth_clientdetails_column_archived", "是否归档"},
                {"oauth_clientdetails_column_create_time", "创建时间"},
                {"oauth_clientdetails_column_client_id", "客户端ID"},
                {"oauth_clientdetails_column_client_secret", "客户端密匙"},
                {"oauth_clientdetails_column_client_name", "客户端名字"},
                {"oauth_clientdetails_column_client_uri", "客户端URI"},
                {"oauth_clientdetails_column_client_icon_uri", "客户端图标URI"},
                {"oauth_clientdetails_column_redirect_uri", "重定向地址"},
                {"oauth_clientdetails_column_description", "描述信息"},
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
                {"客户端详情", "modules/oauth/clientdetails", "oauth:clientdetails:list,oauth:clientdetails:info,oauth:clientdetails:save,oauth:clientdetails:update,oauth:clientdetails:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "oauth_clientdetails_table_comment"},
                {"查看", null, "oauth:clientdetails:list,oauth:clientdetails:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "oauth:clientdetails:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "oauth:clientdetails:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "oauth:clientdetails:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
