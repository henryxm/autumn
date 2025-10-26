package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.lan.interceptor.LanguageInterceptor;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.annotation.SysLog;
import cn.org.autumn.exception.AException;
import cn.org.autumn.utils.R;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/sys/menu")
public class SysMenuController extends AbstractController {
    @Autowired
    @Lazy
    private SysMenuService sysMenuService;

    @Autowired
    @Lazy
    LanguageService languageService;

    @Autowired
    @Lazy
    SuperPositionModelService superPositionModelService;

    public void r(SysMenuEntity sysMenuEntity, Map<String, String> language, Map<String, SuperPositionModelEntity> spms) {
        if (StringUtils.isNotEmpty(sysMenuEntity.getLanguageName()) && language.containsKey(sysMenuEntity.getLanguageName())) {
            sysMenuEntity.setName(language.get(sysMenuEntity.getLanguageName()));
        }

        if (superPositionModelService.menuWithSpm()) {
            if (StringUtils.isNotEmpty(sysMenuEntity.getUrl()) && spms.containsKey(sysMenuEntity.getUrl())) {
                sysMenuEntity.setUrl("?spm=" + spms.get(sysMenuEntity.getUrl()).toSpmString());
            }
        }

        if (null != sysMenuEntity.getList() && sysMenuEntity.getList().size() > 0) {
            try {
                for (SysMenuEntity sub : (List<SysMenuEntity>) sysMenuEntity.getList()) {
                    r(sub, language, spms);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 导航菜单
     */
    @RequestMapping("/nav")
    public R nav(HttpServletRequest request) {
        if (!ShiroUtils.isLogin())
            return R.error("未登录");
        List<SysMenuEntity> menuList = sysMenuService.getUserMenuList(getUserUuid());
        Locale locale = LanguageInterceptor.getLocale(request);
        Map<String, String> language = languageService.getLanguage(locale);
        Map<String, SuperPositionModelEntity> spms = superPositionModelService.getSpmListForResourceID();
        if (null != language && language.size() > 0) {
            for (SysMenuEntity sysMenuEntity : menuList) {
                r(sysMenuEntity, language, spms);
            }
        }
        return R.ok().put("menuList", menuList);
    }

    /**
     * 所有菜单列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:menu:list")
    public List<SysMenuEntity> list() {
        List<SysMenuEntity> menuList = sysMenuService.selectList(null);
        for (SysMenuEntity sysMenuEntity : menuList) {
            SysMenuEntity parentMenuEntity = sysMenuService.getByMenuKey(sysMenuEntity.getParentKey());
            if (parentMenuEntity != null) {
                sysMenuEntity.setParentName(parentMenuEntity.getName());
            }
        }
        return menuList;
    }

    /**
     * 选择菜单(添加、修改菜单)
     */
    @RequestMapping("/select")
    @RequiresPermissions("sys:menu:select")
    public R select(HttpServletRequest request) {
        //查询列表数据
        List<SysMenuEntity> menuList = sysMenuService.queryNotButtonList();
        //添加顶级菜单
        SysMenuEntity root = new SysMenuEntity();
        root.setMenuKey("");
        root.setLanguageName("sys_string_root_menu");
        root.setName("一级菜单");
        root.setParentKey("-1");
        root.setOpen(true);
        menuList.add(root);
        Locale locale = LanguageInterceptor.getLocale(request);
        Map<String, String> language = languageService.getLanguage(locale);
        Map<String, SuperPositionModelEntity> spms = superPositionModelService.getSpmListForResourceID();
        if (null != language && language.size() > 0) {
            for (SysMenuEntity sysMenuEntity : menuList) {
                r(sysMenuEntity, language, spms);
            }
        }
        return R.ok().put("menuList", menuList);
    }

    /**
     * 菜单信息
     */
    @RequestMapping("/info/{menuKey}")
    @RequiresPermissions("sys:menu:info")
    public R info(@PathVariable("menuKey") String menuKey) {
        SysMenuEntity menu = sysMenuService.getByMenuKey(menuKey);
        return R.ok().put("menu", menu);
    }

    /**
     * 保存
     */
    @SysLog("保存菜单")
    @RequestMapping("/save")
    @RequiresPermissions("sys:menu:save")
    public R save(@RequestBody SysMenuEntity menu) {
        //数据校验
        verifyForm(menu);
        if (null == menu.getParentKey())
            menu.setParentKey("");
        sysMenuService.insertOrUpdate(menu);
        return R.ok();
    }

    /**
     * 删除
     */
    @SysLog("删除菜单")
    @RequestMapping("/delete")
    @RequiresPermissions("sys:menu:delete")
    public R delete(String menuKey) {

        //判断是否有子菜单或按钮
        List<SysMenuEntity> menuList = sysMenuService.getByParentKey(menuKey);
        if (menuList.size() > 0) {
            return R.error("请先删除子菜单或按钮");
        }

        sysMenuService.delete(menuKey);

        return R.ok();
    }

    /**
     * 验证参数是否正确
     */
    private void verifyForm(SysMenuEntity menu) {
        if (StringUtils.isBlank(menu.getName())) {
            throw new AException("菜单名称不能为空");
        }

        if (null == menu.getParentKey()) {
            throw new AException("上级菜单不能为空");
        }

        //菜单
        if (menu.getType() == Constant.MenuType.MENU.getValue()) {
            if (StringUtils.isBlank(menu.getUrl())) {
                throw new AException("菜单URL不能为空");
            }
        }

        //上级菜单类型
        int parentType = Constant.MenuType.CATALOG.getValue();
        if (StringUtils.isNotEmpty(menu.getParentKey())) {
            SysMenuEntity parentMenu = sysMenuService.getByMenuKey(menu.getParentKey());
            parentType = parentMenu.getType();
        }

        //目录、菜单
        if (menu.getType() == Constant.MenuType.CATALOG.getValue() ||
                menu.getType() == Constant.MenuType.MENU.getValue()) {
            if (parentType != Constant.MenuType.CATALOG.getValue()) {
                throw new AException("上级菜单只能为目录类型");
            }
            return;
        }

        //按钮
        if (menu.getType() == Constant.MenuType.BUTTON.getValue()) {
            if (parentType != Constant.MenuType.MENU.getValue()) {
                throw new AException("上级菜单只能为菜单类型");
            }
            return;
        }
    }
}
