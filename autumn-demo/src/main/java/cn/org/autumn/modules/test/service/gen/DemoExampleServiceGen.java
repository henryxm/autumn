package cn.org.autumn.modules.test.service.gen;

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
import cn.org.autumn.modules.test.service.TestMenu;
import cn.org.autumn.modules.test.dao.DemoExampleDao;
import cn.org.autumn.modules.test.entity.DemoExampleEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 测试例子控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class DemoExampleServiceGen extends ServiceImpl<DemoExampleDao, DemoExampleEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected TestMenu testMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<DemoExampleEntity> _page = new Query<DemoExampleEntity>(params).getPage();
        EntityWrapper<DemoExampleEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("example") && null !=params.get("example") && StringUtils.isNotEmpty(params.get("example").toString())) {
            condition.put("example", params.get("example"));
        }
        _page.setCondition(condition);
        Page<DemoExampleEntity> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    /**
    * need implement it in the subclass.
    * @return
    */
    public int menuOrder(){
        return 0;
    }

    /**
    * need implement it in the subclass.
    * @return
    */
    public String parentMenu(){
        testMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(TestMenu.test_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String ico(){
        return "fa-file-code-o";
    }

    protected String order(){
        return String.valueOf(menuOrder());
    }

    public void init() {
        sysMenuService.put(getMenus());
        language.add(getLanguageItemArray());
        language.add(getLanguageItems());
        addLanguageColumnItem();
        language.add(getLanguageItemsInternal());
    }

    public String[][] getLanguageItemArray() {
        return null;
    }

    public List<String[]> getLanguageItems() {
        return null;
    }

    public void addLanguageColumnItem(){
    }

    public String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {"test_demoexample_table_comment", "测试例子"},
                {"test_demoexample_column_id", "ID"},
                {"test_demoexample_column_example", "例子字段"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Test", "DemoExample");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"测试例子", "modules/test/demoexample", "test:demoexample:list,test:demoexample:info,test:demoexample:save,test:demoexample:update,test:demoexample:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "test_demoexample_table_comment"},
                {"查看", null, "test:demoexample:list,test:demoexample:info", "2", null, order(), SysMenuService.getMenuKey("Test", "DemoExampleInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "test:demoexample:save", "2", null, order(), SysMenuService.getMenuKey("Test", "DemoExampleSave"), menuKey, "sys_string_add"},
                {"修改", null, "test:demoexample:update", "2", null, order(), SysMenuService.getMenuKey("Test", "DemoExampleUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "test:demoexample:delete", "2", null, order(), SysMenuService.getMenuKey("Test", "DemoExampleDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
