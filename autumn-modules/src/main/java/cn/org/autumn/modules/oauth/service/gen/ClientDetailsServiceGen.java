package cn.org.autumn.modules.oauth.service.gen;

import cn.org.autumn.table.TableInit;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.oauth.service.OauthMenu;
import cn.org.autumn.modules.oauth.dao.ClientDetailsDao;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 客户端详情控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class ClientDetailsServiceGen extends ServiceImpl<ClientDetailsDao, ClientDetailsEntity> {

    protected static final String NULL = null;

    @Autowired
    protected OauthMenu oauthMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<ClientDetailsEntity> _page = new Query<ClientDetailsEntity>(params).getPage();
        EntityWrapper<ClientDetailsEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("resourceIds") && null !=params.get("resourceIds") && StringUtils.isNotEmpty(params.get("resourceIds").toString())) {
            condition.put("resource_ids", params.get("resourceIds"));
        }
        if(params.containsKey("scope") && null !=params.get("scope") && StringUtils.isNotEmpty(params.get("scope").toString())) {
            condition.put("scope", params.get("scope"));
        }
        if(params.containsKey("grantTypes") && null !=params.get("grantTypes") && StringUtils.isNotEmpty(params.get("grantTypes").toString())) {
            condition.put("grant_types", params.get("grantTypes"));
        }
        if(params.containsKey("roles") && null !=params.get("roles") && StringUtils.isNotEmpty(params.get("roles").toString())) {
            condition.put("roles", params.get("roles"));
        }
        if(params.containsKey("trusted") && null !=params.get("trusted") && StringUtils.isNotEmpty(params.get("trusted").toString())) {
            condition.put("trusted", params.get("trusted"));
        }
        if(params.containsKey("archived") && null !=params.get("archived") && StringUtils.isNotEmpty(params.get("archived").toString())) {
            condition.put("archived", params.get("archived"));
        }
        if(params.containsKey("createTime") && null !=params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        if(params.containsKey("clientId") && null !=params.get("clientId") && StringUtils.isNotEmpty(params.get("clientId").toString())) {
            condition.put("client_id", params.get("clientId"));
        }
        if(params.containsKey("clientSecret") && null !=params.get("clientSecret") && StringUtils.isNotEmpty(params.get("clientSecret").toString())) {
            condition.put("client_secret", params.get("clientSecret"));
        }
        if(params.containsKey("clientName") && null !=params.get("clientName") && StringUtils.isNotEmpty(params.get("clientName").toString())) {
            condition.put("client_name", params.get("clientName"));
        }
        if(params.containsKey("clientUri") && null !=params.get("clientUri") && StringUtils.isNotEmpty(params.get("clientUri").toString())) {
            condition.put("client_uri", params.get("clientUri"));
        }
        if(params.containsKey("clientIconUri") && null !=params.get("clientIconUri") && StringUtils.isNotEmpty(params.get("clientIconUri").toString())) {
            condition.put("client_icon_uri", params.get("clientIconUri"));
        }
        if(params.containsKey("redirectUri") && null !=params.get("redirectUri") && StringUtils.isNotEmpty(params.get("redirectUri").toString())) {
            condition.put("redirect_uri", params.get("redirectUri"));
        }
        if(params.containsKey("description") && null !=params.get("description") && StringUtils.isNotEmpty(params.get("description").toString())) {
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
    public int parentMenu(){
        oauthMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(OauthMenu.oauth_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 90;
    }

    public String ico(){
        return "fa-file-code-o";
    }

    private String order(){
        return String.valueOf(menuOrder());
    }

    private String parent(){
        return String.valueOf(parentMenu());
    }

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, parent(), "客户端详情", "modules/oauth/clientdetails", "oauth:clientdetails:list,oauth:clientdetails:info,oauth:clientdetails:save,oauth:clientdetails:update,oauth:clientdetails:delete", "1", "fa " + ico(), order(), "", "oauth_clientdetails_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "oauth:clientdetails:list,oauth:clientdetails:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "oauth:clientdetails:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "oauth:clientdetails:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "oauth:clientdetails:delete", "2", null, order(), "", "sys_string_delete"},
        };
        for (String[] menu : menus) {
            sysMenu = sysMenuService.from(menu);
            entity = sysMenuService.get(sysMenu);
            if (null == entity) {
                sysMenuService.put(sysMenu);
            }
        }
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("oauth_clientdetails_table_comment", "客户端详情");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_id", "id");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_resource_ids", "资源ID");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_scope", "范围");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_grant_types", "授权类型");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_roles", "角色");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_trusted", "是否可信");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_archived", "是否归档");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_create_time", "创建时间");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_id", "客户端ID");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_secret", "客户端密匙");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_name", "客户端名字");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_uri", "客户端URI");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_client_icon_uri", "客户端图标URI");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_redirect_uri", "重定向地址");
        languageService.addLanguageColumnItem("oauth_clientdetails_column_description", "描述信息");
    }
}
