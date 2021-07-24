package cn.org.autumn.modules.wall.service.gen;

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
import cn.org.autumn.modules.wall.service.WallMenu;
import cn.org.autumn.modules.wall.dao.HostDao;
import cn.org.autumn.modules.wall.entity.HostEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 主机统计控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-07
 */
public class HostServiceGen extends ServiceImpl<HostDao, HostEntity> implements InitFactory.Init {

    @Autowired
    protected WallMenu wallMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<HostEntity> _page = new Query<HostEntity>(params).getPage();
        EntityWrapper<HostEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("host") && null != params.get("host") && StringUtils.isNotEmpty(params.get("host").toString())) {
            condition.put("host", params.get("host"));
        }
        if(params.containsKey("count") && null != params.get("count") && StringUtils.isNotEmpty(params.get("count").toString())) {
            condition.put("count", params.get("count"));
        }
        if(params.containsKey("forbidden") && null != params.get("forbidden") && StringUtils.isNotEmpty(params.get("forbidden").toString())) {
            condition.put("forbidden", params.get("forbidden"));
        }
        if(params.containsKey("tag") && null != params.get("tag") && StringUtils.isNotEmpty(params.get("tag").toString())) {
            condition.put("tag", params.get("tag"));
        }
        if(params.containsKey("description") && null != params.get("description") && StringUtils.isNotEmpty(params.get("description").toString())) {
            condition.put("description", params.get("description"));
        }
        _page.setCondition(condition);
        Page<HostEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        wallMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(wallMenu.getMenu());
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Wall", "Host");
        return menu;
    }

    public String button(String button) {
        String menu = menu() + button;
        return menu;
    }

    public String ico(){
        return "fa-file-code-o";
    }

    protected String order(){
        return String.valueOf(menuOrder());
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    private String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {"wall_host_table_comment", "主机统计", "Host"},
                {"wall_host_column_id", "id", "Id"},
                {"wall_host_column_host", "主机地址", "Host"},
                {"wall_host_column_count", "访问次数", "Count"},
                {"wall_host_column_forbidden", "禁用", "Forbidden"},
                {"wall_host_column_tag", "标签说明", "Tag"},
                {"wall_host_column_description", "描述信息", "Description"},
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
                {"主机统计", "modules/wall/host", "wall:host:list,wall:host:info,wall:host:save,wall:host:update,wall:host:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "wall_host_table_comment"},
                {"查看", null, "wall:host:list,wall:host:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "wall:host:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "wall:host:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "wall:host:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
