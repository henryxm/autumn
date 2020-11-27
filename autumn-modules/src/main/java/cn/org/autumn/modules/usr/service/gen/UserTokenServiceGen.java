package cn.org.autumn.modules.usr.service.gen;

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
import cn.org.autumn.modules.usr.service.UsrMenu;
import cn.org.autumn.modules.usr.dao.UserTokenDao;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 用户Token控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class UserTokenServiceGen extends ServiceImpl<UserTokenDao, UserTokenEntity> {

    protected static final String NULL = null;

    @Autowired
    protected UsrMenu usrMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

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
    public int parentMenu(){
        usrMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(UsrMenu.usr_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 66;
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
                {null, parent(), "用户Token", "modules/usr/usertoken", "usr:usertoken:list,usr:usertoken:info,usr:usertoken:save,usr:usertoken:update,usr:usertoken:delete", "1", "fa " + ico(), order(), "", "usr_usertoken_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "usr:usertoken:list,usr:usertoken:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "usr:usertoken:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "usr:usertoken:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "usr:usertoken:delete", "2", null, order(), "", "sys_string_delete"},
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
