package cn.org.autumn.menu;

import cn.org.autumn.site.InitFactory;
import org.apache.commons.lang.StringUtils;

public interface BaseMenu extends InitFactory.Init {
    default String order() {
        Menu menu = getClass().getAnnotation(Menu.class);
        if (null != menu)
            return String.valueOf(menu.order());
        return "0";
    }

    default String ico() {
        Menu menu = getClass().getAnnotation(Menu.class);
        if (null != menu)
            return menu.ico();
        return "fa-file-code-o";
    }

    String getMenu();

    default String getParentMenu() {
        return "";
    }

    default String getNamespace() {
        Menu menu = getClass().getAnnotation(Menu.class);
        String namespace = null;
        if (null != menu)
            namespace = menu.namespace();
        if (StringUtils.isBlank(namespace)) {
            namespace = getClass().getSimpleName();
            if (namespace.toLowerCase().endsWith("menu"))
                namespace = namespace.substring(0, namespace.length() - 4);
        }
        return namespace;
    }

    default String getMenuKey() {
        return getClass().getSimpleName();
    }

    default String getName() {
        Menu menu = getClass().getAnnotation(Menu.class);
        if (null != menu)
            return menu.name();
        return getMenuKey();
    }

    default String getLanguageKey() {
        return getNamespace().toLowerCase() + "_menu_text";
    }
}