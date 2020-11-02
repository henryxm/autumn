package cn.org.autumn.modules.spm.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 超级位置模型
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Service
public class SpmMenu {

    public static final String spm_menu = "spm_menu";

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
                {null, "0" , "超级模型" , "" , "" , "0" , "fa fa-address-card-o" , "0" , spm_menu, spm_menu + "_text"};
        SysMenuEntity entity = sysMenuService.get(sysMenuService.find(_m));
        if (null == entity) {
            SysMenuEntity sysMenu = sysMenuService.from(_m);
            sysMenuService.put(sysMenu);
        }
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem(spm_menu + "_text", "超级模型","Super model");
    }
}
