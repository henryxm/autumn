package cn.org.autumn.modules.test.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 例子
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

    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, "0" , "测试" , "" , "" , "0" , "fa fa-address-card-o" , "0" , test_menu};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            sysMenuService.put(sysMenu);
        }
    }
}
