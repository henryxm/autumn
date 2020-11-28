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
import cn.org.autumn.modules.usr.dao.UserLoginLogDao;
import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;

import javax.annotation.PostConstruct;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 登录日志控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class UserLoginLogServiceGen extends ServiceImpl<UserLoginLogDao, UserLoginLogEntity> {

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
        Page<UserLoginLogEntity> _page = new Query<UserLoginLogEntity>(params).getPage();
        EntityWrapper<UserLoginLogEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();
        if (params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if (params.containsKey("userId") && null != params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if (params.containsKey("username") && null != params.get("username") && StringUtils.isNotEmpty(params.get("username").toString())) {
            condition.put("username", params.get("username"));
        }
        if (params.containsKey("loginTime") && null != params.get("loginTime") && StringUtils.isNotEmpty(params.get("loginTime").toString())) {
            condition.put("login_time", params.get("loginTime"));
        }
        if (params.containsKey("logoutTime") && null != params.get("logoutTime") && StringUtils.isNotEmpty(params.get("logoutTime").toString())) {
            condition.put("logout_time", params.get("logoutTime"));
        }
        _page.setCondition(condition);
        Page<UserLoginLogEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        usrMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(UsrMenu.usr_menu);
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 102;
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
                {null, parent(), "登录日志", "modules/usr/userloginlog", "usr:userloginlog:list,usr:userloginlog:info,usr:userloginlog:save,usr:userloginlog:update,usr:userloginlog:delete", "1", "fa " + ico(), order(), "", "usr_userloginlog_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "usr:userloginlog:list,usr:userloginlog:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "usr:userloginlog:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "usr:userloginlog:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "usr:userloginlog:delete", "2", null, order(), "", "sys_string_delete"},
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
