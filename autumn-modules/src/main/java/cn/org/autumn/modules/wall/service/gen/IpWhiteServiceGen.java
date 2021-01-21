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
import cn.org.autumn.modules.wall.dao.IpWhiteDao;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * IP白名单控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class IpWhiteServiceGen extends ServiceImpl<IpWhiteDao, IpWhiteEntity> implements InitFactory.Init {

    @Autowired
    protected WallMenu wallMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<IpWhiteEntity> _page = new Query<IpWhiteEntity>(params).getPage();
        EntityWrapper<IpWhiteEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("ip") && null != params.get("ip") && StringUtils.isNotEmpty(params.get("ip").toString())) {
            condition.put("ip", params.get("ip"));
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
        if(params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        if(params.containsKey("updateTime") && null != params.get("updateTime") && StringUtils.isNotEmpty(params.get("updateTime").toString())) {
            condition.put("update_time", params.get("updateTime"));
        }
        _page.setCondition(condition);
        Page<IpWhiteEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        String menu = SysMenuService.getMenuKey("Wall", "IpWhite");
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
                {"wall_ipwhite_table_comment", "IP白名单"},
                {"wall_ipwhite_column_id", "id"},
                {"wall_ipwhite_column_ip", "IP地址"},
                {"wall_ipwhite_column_count", "访问次数"},
                {"wall_ipwhite_column_forbidden", "禁用"},
                {"wall_ipwhite_column_tag", "标签说明"},
                {"wall_ipwhite_column_description", "描述信息"},
                {"wall_ipwhite_column_create_time", "创建时间"},
                {"wall_ipwhite_column_update_time", "更新时间"},
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
                {"IP白名单", "modules/wall/ipwhite", "wall:ipwhite:list,wall:ipwhite:info,wall:ipwhite:save,wall:ipwhite:update,wall:ipwhite:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "wall_ipwhite_table_comment"},
                {"查看", null, "wall:ipwhite:list,wall:ipwhite:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "wall:ipwhite:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "wall:ipwhite:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "wall:ipwhite:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
