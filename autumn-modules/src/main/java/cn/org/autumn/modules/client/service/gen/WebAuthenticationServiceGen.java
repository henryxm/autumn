package cn.org.autumn.modules.client.service.gen;

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
import cn.org.autumn.modules.client.service.ClientMenu;
import cn.org.autumn.modules.client.dao.WebAuthenticationDao;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 网站客户端控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class WebAuthenticationServiceGen extends ServiceImpl<WebAuthenticationDao, WebAuthenticationEntity> {

    protected static final String NULL = null;

    @Autowired
    protected ClientMenu clientMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

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
    public int parentMenu(){
        clientMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(ClientMenu.client_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 77;
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
                {null, parent(), "网站客户端", "modules/client/webauthentication", "client:webauthentication:list,client:webauthentication:info,client:webauthentication:save,client:webauthentication:update,client:webauthentication:delete", "1", "fa " + ico(), order(), "", "client_webauthentication_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "client:webauthentication:list,client:webauthentication:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "client:webauthentication:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "client:webauthentication:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "client:webauthentication:delete", "2", null, order(), "", "sys_string_delete"},
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
     }
}
