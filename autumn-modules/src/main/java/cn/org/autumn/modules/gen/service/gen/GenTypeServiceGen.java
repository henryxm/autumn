package cn.org.autumn.modules.gen.service.gen;

import cn.org.autumn.site.InitFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.gen.service.GenMenu;
import cn.org.autumn.modules.gen.dao.GenTypeDao;
import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 生成方案控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class GenTypeServiceGen extends ServiceImpl<GenTypeDao, GenTypeEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected GenMenu genMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<GenTypeEntity> _page = new Query<GenTypeEntity>(params).getPage();
        EntityWrapper<GenTypeEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();
        if (params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if (params.containsKey("databaseType") && null != params.get("databaseType") && StringUtils.isNotEmpty(params.get("databaseType").toString())) {
            condition.put("database_type", params.get("databaseType"));
        }
        if (params.containsKey("rootPackage") && null != params.get("rootPackage") && StringUtils.isNotEmpty(params.get("rootPackage").toString())) {
            condition.put("root_package", params.get("rootPackage"));
        }
        if (params.containsKey("modulePackage") && null != params.get("modulePackage") && StringUtils.isNotEmpty(params.get("modulePackage").toString())) {
            condition.put("module_package", params.get("modulePackage"));
        }
        if (params.containsKey("moduleName") && null != params.get("moduleName") && StringUtils.isNotEmpty(params.get("moduleName").toString())) {
            condition.put("module_name", params.get("moduleName"));
        }
        if (params.containsKey("moduleText") && null != params.get("moduleText") && StringUtils.isNotEmpty(params.get("moduleText").toString())) {
            condition.put("module_text", params.get("moduleText"));
        }
        if (params.containsKey("moduleId") && null != params.get("moduleId") && StringUtils.isNotEmpty(params.get("moduleId").toString())) {
            condition.put("module_id", params.get("moduleId"));
        }
        if (params.containsKey("authorName") && null != params.get("authorName") && StringUtils.isNotEmpty(params.get("authorName").toString())) {
            condition.put("author_name", params.get("authorName"));
        }
        if (params.containsKey("email") && null != params.get("email") && StringUtils.isNotEmpty(params.get("email").toString())) {
            condition.put("email", params.get("email"));
        }
        if (params.containsKey("tablePrefix") && null != params.get("tablePrefix") && StringUtils.isNotEmpty(params.get("tablePrefix").toString())) {
            condition.put("table_prefix", params.get("tablePrefix"));
        }
        if (params.containsKey("mappingString") && null != params.get("mappingString") && StringUtils.isNotEmpty(params.get("mappingString").toString())) {
            condition.put("mapping_string", params.get("mappingString"));
        }
        _page.setCondition(condition);
        Page<GenTypeEntity> page = this.selectPage(_page, entityEntityWrapper);
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
    public String parentMenu() {
        genMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(GenMenu.gen_menu);
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Gen", "GenType");
        return menu;
    }

    public String button(String button) {
        String menu = SysMenuService.getMenuKey("Gen", "GenType" + button);
        return menu;
    }

    public String ico() {
        return "fa-file-code-o";
    }

    protected String order() {
        return String.valueOf(menuOrder());
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
        addLanguageColumnItem();
    }

    @Deprecated
    public void addLanguageColumnItem() {
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    private String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {"gen_gentype_table_comment", "生成方案"},
                {"gen_gentype_column_id", "序列号"},
                {"gen_gentype_column_database_type", "数据库类型"},
                {"gen_gentype_column_root_package", "程序根包名"},
                {"gen_gentype_column_module_package", "模块根包名"},
                {"gen_gentype_column_module_name", "模块名(用于包名)"},
                {"gen_gentype_column_module_text", "模块名称(用于目录)"},
                {"gen_gentype_column_module_id", "模块ID(用于目录)"},
                {"gen_gentype_column_author_name", "作者名字"},
                {"gen_gentype_column_email", "作者邮箱"},
                {"gen_gentype_column_table_prefix", "表前缀"},
                {"gen_gentype_column_mapping_string", "表字段映射"},
        };
        return items;
    }

    public List<String[]> getMenuList() {
        return null;
    }

    public String[][] getMenuItems() {
        return null;
    }

    private String[][] getMenuItemsInternal() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"生成方案", "modules/gen/gentype", "gen:gentype:list,gen:gentype:info,gen:gentype:save,gen:gentype:update,gen:gentype:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "gen_gentype_table_comment"},
                {"查看", null, "gen:gentype:list,gen:gentype:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "gen:gentype:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "gen:gentype:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "gen:gentype:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
