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
import cn.org.autumn.modules.wall.dao.IpBlackDao;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * IP黑名单控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class IpBlackServiceGen extends ServiceImpl<IpBlackDao, IpBlackEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected WallMenu wallMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<IpBlackEntity> _page = new Query<IpBlackEntity>(params).getPage();
        EntityWrapper<IpBlackEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("ip") && null !=params.get("ip") && StringUtils.isNotEmpty(params.get("ip").toString())) {
            condition.put("ip", params.get("ip"));
        }
        if(params.containsKey("count") && null !=params.get("count") && StringUtils.isNotEmpty(params.get("count").toString())) {
            condition.put("count", params.get("count"));
        }
        if(params.containsKey("available") && null !=params.get("available") && StringUtils.isNotEmpty(params.get("available").toString())) {
            condition.put("available", params.get("available"));
        }
        if(params.containsKey("tag") && null !=params.get("tag") && StringUtils.isNotEmpty(params.get("tag").toString())) {
            condition.put("tag", params.get("tag"));
        }
        if(params.containsKey("description") && null !=params.get("description") && StringUtils.isNotEmpty(params.get("description").toString())) {
            condition.put("description", params.get("description"));
        }
        if(params.containsKey("createTime") && null !=params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        if(params.containsKey("updateTime") && null !=params.get("updateTime") && StringUtils.isNotEmpty(params.get("updateTime").toString())) {
            condition.put("update_time", params.get("updateTime"));
        }
        _page.setCondition(condition);
        Page<IpBlackEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(WallMenu.wall_menu);
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
                {"wall_ipblack_table_comment", "IP黑名单"},
                {"wall_ipblack_column_id", "id"},
                {"wall_ipblack_column_ip", "IP地址"},
                {"wall_ipblack_column_count", "访问次数"},
                {"wall_ipblack_column_available", "可用"},
                {"wall_ipblack_column_tag", "标签说明"},
                {"wall_ipblack_column_description", "描述信息"},
                {"wall_ipblack_column_create_time", "创建时间"},
                {"wall_ipblack_column_update_time", "更新时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Wall", "IpBlack");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"IP黑名单", "modules/wall/ipblack", "wall:ipblack:list,wall:ipblack:info,wall:ipblack:save,wall:ipblack:update,wall:ipblack:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "wall_ipblack_table_comment"},
                {"查看", null, "wall:ipblack:list,wall:ipblack:info", "2", null, order(), SysMenuService.getMenuKey("Wall", "IpBlackInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "wall:ipblack:save", "2", null, order(), SysMenuService.getMenuKey("Wall", "IpBlackSave"), menuKey, "sys_string_add"},
                {"修改", null, "wall:ipblack:update", "2", null, order(), SysMenuService.getMenuKey("Wall", "IpBlackUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "wall:ipblack:delete", "2", null, order(), SysMenuService.getMenuKey("Wall", "IpBlackDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
