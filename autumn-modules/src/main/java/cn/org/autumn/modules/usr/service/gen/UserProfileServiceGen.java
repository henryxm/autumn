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
import cn.org.autumn.modules.usr.dao.UserProfileDao;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;

import javax.annotation.PostConstruct;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 用户信息控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class UserProfileServiceGen extends ServiceImpl<UserProfileDao, UserProfileEntity> {

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
        Page<UserProfileEntity> _page = new Query<UserProfileEntity>(params).getPage();
        EntityWrapper<UserProfileEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();
        if (params.containsKey("userId") && null != params.get("userId") && StringUtils.isNotEmpty(params.get("userId").toString())) {
            condition.put("user_id", params.get("userId"));
        }
        if (params.containsKey("sysUserId") && null != params.get("sysUserId") && StringUtils.isNotEmpty(params.get("sysUserId").toString())) {
            condition.put("sys_user_id", params.get("sysUserId"));
        }
        if (params.containsKey("uuid") && null != params.get("uuid") && StringUtils.isNotEmpty(params.get("uuid").toString())) {
            condition.put("uuid", params.get("uuid"));
        }
        if (params.containsKey("openId") && null != params.get("openId") && StringUtils.isNotEmpty(params.get("openId").toString())) {
            condition.put("open_id", params.get("openId"));
        }
        if (params.containsKey("unionId") && null != params.get("unionId") && StringUtils.isNotEmpty(params.get("unionId").toString())) {
            condition.put("union_id", params.get("unionId"));
        }
        if (params.containsKey("icon") && null != params.get("icon") && StringUtils.isNotEmpty(params.get("icon").toString())) {
            condition.put("icon", params.get("icon"));
        }
        if (params.containsKey("username") && null != params.get("username") && StringUtils.isNotEmpty(params.get("username").toString())) {
            condition.put("username", params.get("username"));
        }
        if (params.containsKey("nickname") && null != params.get("nickname") && StringUtils.isNotEmpty(params.get("nickname").toString())) {
            condition.put("nickname", params.get("nickname"));
        }
        if (params.containsKey("mobile") && null != params.get("mobile") && StringUtils.isNotEmpty(params.get("mobile").toString())) {
            condition.put("mobile", params.get("mobile"));
        }
        if (params.containsKey("password") && null != params.get("password") && StringUtils.isNotEmpty(params.get("password").toString())) {
            condition.put("password", params.get("password"));
        }
        if (params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<UserProfileEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        return 108;
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
                {null, parent(), "用户信息", "modules/usr/userprofile", "usr:userprofile:list,usr:userprofile:info,usr:userprofile:save,usr:userprofile:update,usr:userprofile:delete", "1", "fa " + ico(), order(), "", "usr_userprofile_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "usr:userprofile:list,usr:userprofile:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "usr:userprofile:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "usr:userprofile:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "usr:userprofile:delete", "2", null, order(), "", "sys_string_delete"},
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
