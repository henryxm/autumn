package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 防火墙
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Service
public class WallMenu {

    public static final String wall_menu = "wall_menu";

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
                {null, "0", "防火墙", "", "", "0", "fa fa-firefox", "777777", wall_menu, wall_menu + "_text"};
        SysMenuEntity entity = sysMenuService.get(sysMenuService.find(_m));
        if (null == entity) {
            SysMenuEntity sysMenu = sysMenuService.from(_m);
            sysMenuService.put(sysMenu);
        }
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem(wall_menu + "_text", "防火墙", "Fire wall");
    }
}
