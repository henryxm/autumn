package cn.org.autumn.modules.gen.service.gen;

import cn.org.autumn.table.TableInit;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;

import cn.org.autumn.modules.gen.service.GenMenu;
import cn.org.autumn.modules.gen.dao.GenTypeDao;
import cn.org.autumn.modules.gen.entity.GenTypeEntity;

import javax.annotation.PostConstruct;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 生成方案控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

public class GenTypeServiceGen extends ServiceImpl<GenTypeDao, GenTypeEntity> {

    protected static final String NULL = null;

    @Autowired
    protected GenMenu genMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

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
    public int parentMenu() {
        genMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(GenMenu.gen_menu);
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 1;
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
                {null, parent(), "生成方案", "modules/gen/gentype", "gen:gentype:list,gen:gentype:info,gen:gentype:save,gen:gentype:update,gen:gentype:delete", "1", "fa " + ico(), order(), "", "gen_gentype_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "gen:gentype:list,gen:gentype:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "gen:gentype:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "gen:gentype:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "gen:gentype:delete", "2", null, order(), "", "sys_string_delete"},
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
        languageService.addLanguageColumnItem("gen_gentype_table_comment", "生成方案", "Generate solution");
        languageService.addLanguageColumnItem("gen_gentype_column_id", "序列号", "Serial number");
        languageService.addLanguageColumnItem("gen_gentype_column_database_type", "数据库类型", "Database type");
        languageService.addLanguageColumnItem("gen_gentype_column_root_package", "程序根包名", "Root package name");
        languageService.addLanguageColumnItem("gen_gentype_column_module_package", "模块根包名", "Module package name");
        languageService.addLanguageColumnItem("gen_gentype_column_module_name", "模块名(用于包名)", "Module name");
        languageService.addLanguageColumnItem("gen_gentype_column_module_text", "模块名称(用于目录)", "Module menu name");
        languageService.addLanguageColumnItem("gen_gentype_column_module_id", "模块ID(用于目录)", "Module ID");
        languageService.addLanguageColumnItem("gen_gentype_column_author_name", "作者名字", "Author name");
        languageService.addLanguageColumnItem("gen_gentype_column_email", "作者邮箱", "Author email");
        languageService.addLanguageColumnItem("gen_gentype_column_table_prefix", "表前缀", "Table prefix");
        languageService.addLanguageColumnItem("gen_gentype_column_mapping_string", "表字段映射", "Table mapping");
    }
}
