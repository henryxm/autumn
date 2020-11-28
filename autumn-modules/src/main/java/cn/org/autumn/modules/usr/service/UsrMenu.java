package cn.org.autumn.modules.usr.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Service
public class UsrMenu {

    public static final String usr_menu = "usr_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

    @Autowired
    protected LanguageService languageService;

    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, "0" , "用户管理" , "" , "" , "0" , "fa fa-users" , "444444" , usr_menu, usr_menu + "_text"};
        SysMenuEntity entity = sysMenuService.get(sysMenuService.find(_m));
        if (null == entity) {
            SysMenuEntity sysMenu = sysMenuService.from(_m);
            sysMenuService.put(sysMenu);
        }
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem(usr_menu + "_text", "用户管理","User profiles");
    }
}
