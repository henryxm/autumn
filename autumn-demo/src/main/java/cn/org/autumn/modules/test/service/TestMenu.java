package cn.org.autumn.modules.test.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 测试例子
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@Service
public class TestMenu {

    public static final String test_menu = "test_menu";

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
                {null, "0", "测试", "", "", "0", "fa fa-address-card-o", "0", test_menu, test_menu + "_text"};
        SysMenuEntity entity = sysMenuService.get(sysMenuService.find(_m));
        if (null == entity) {
            SysMenuEntity sysMenu = sysMenuService.from(_m);
            sysMenuService.put(sysMenu);
        }
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem(test_menu + "_text", "测试", "Testing");
    }
}
