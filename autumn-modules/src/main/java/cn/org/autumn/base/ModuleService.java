package cn.org.autumn.base;

import cn.org.autumn.config.Config;
import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.service.BaseService;
import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ModuleService<M extends BaseMapper<T>, T> extends BaseService<M, T> implements InitFactory.Init {

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    protected BaseMenu baseMenu;

    public BaseMenu getBaseMenu() {
        if (null != baseMenu)
            return baseMenu;
        String bean = getPrefix() + "Menu";
        Object o = Config.getBean(bean);
        if (o instanceof BaseMenu)
            baseMenu = (BaseMenu) o;
        return baseMenu;
    }

    public String parentMenu() {
        getBaseMenu().init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(getBaseMenu().getMenu());
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        Class<?> clazz = getModelClass();
        String menuKey = clazz.getSimpleName();
        if (menuKey.toLowerCase().endsWith("entity")) {
            menuKey = menuKey.substring(0, menuKey.length() - 6);
        }
        return SysMenuService.getMenuKey(getBaseMenu().getNamespace(), menuKey);
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }
}