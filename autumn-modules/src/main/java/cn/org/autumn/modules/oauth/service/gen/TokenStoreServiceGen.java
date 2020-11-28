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
import cn.org.autumn.modules.oauth.dao.TokenStoreDao;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;

import javax.annotation.PostConstruct;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 授权令牌控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class TokenStoreServiceGen extends ServiceImpl<TokenStoreDao, TokenStoreEntity> {

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
        Page<TokenStoreEntity> _page = new Query<TokenStoreEntity>(params).getPage();
        EntityWrapper<TokenStoreEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();
        if (params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if (params.containsKey("userId") && null != params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if (params.containsKey("userUuid") && null != params.get("userUuid") && StringUtils.isNotEmpty(params.get("userUuid").toString())) {
            condition.put("user_uuid", params.get("userUuid"));
        }
        if (params.containsKey("authCode") && null != params.get("authCode") && StringUtils.isNotEmpty(params.get("authCode").toString())) {
            condition.put("auth_code", params.get("authCode"));
        }
        if (params.containsKey("accessToken") && null != params.get("accessToken") && StringUtils.isNotEmpty(params.get("accessToken").toString())) {
            condition.put("access_token", params.get("accessToken"));
        }
        if (params.containsKey("accessTokenExpiredIn") && null != params.get("accessTokenExpiredIn") && StringUtils.isNotEmpty(params.get("accessTokenExpiredIn").toString())) {
            condition.put("access_token_expired_in", params.get("accessTokenExpiredIn"));
        }
        if (params.containsKey("refreshToken") && null != params.get("refreshToken") && StringUtils.isNotEmpty(params.get("refreshToken").toString())) {
            condition.put("refresh_token", params.get("refreshToken"));
        }
        if (params.containsKey("refreshTokenExpiredIn") && null != params.get("refreshTokenExpiredIn") && StringUtils.isNotEmpty(params.get("refreshTokenExpiredIn").toString())) {
            condition.put("refresh_token_expired_in", params.get("refreshTokenExpiredIn"));
        }
        if (params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<TokenStoreEntity> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    /**
     * need implement it in the subclass.
     *
     * @return
     */
    public int menuOrder() {
        return 0;
    }

    /**
     * need implement it in the subclass.
     *
     * @return
     */
    public int parentMenu() {
        oauthMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(OauthMenu.oauth_menu);
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 45;
    }

    public String ico() {
        return "fa-file-code-o";
    }

    private String order() {
        return String.valueOf(menuOrder());
    }

    private String parent() {
        return String.valueOf(parentMenu());
    }

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, parent(), "授权令牌", "modules/oauth/tokenstore", "oauth:tokenstore:list,oauth:tokenstore:info,oauth:tokenstore:save,oauth:tokenstore:update,oauth:tokenstore:delete", "1", "fa " + ico(), order(), "", "oauth_tokenstore_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "oauth:tokenstore:list,oauth:tokenstore:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "oauth:tokenstore:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "oauth:tokenstore:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "oauth:tokenstore:delete", "2", null, order(), "", "sys_string_delete"},
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
