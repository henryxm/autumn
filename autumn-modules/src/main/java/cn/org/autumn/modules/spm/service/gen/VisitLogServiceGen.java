package cn.org.autumn.modules.spm.service.gen;

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
import cn.org.autumn.modules.spm.service.SpmMenu;
import cn.org.autumn.modules.spm.dao.VisitLogDao;
import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 访问统计控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class VisitLogServiceGen extends ServiceImpl<VisitLogDao, VisitLogEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    protected SpmMenu spmMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<VisitLogEntity> _page = new Query<VisitLogEntity>(params).getPage();
        EntityWrapper<VisitLogEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("siteId") && null !=params.get("siteId") && StringUtils.isNotEmpty(params.get("siteId").toString())) {
            condition.put("site_id", params.get("siteId"));
        }
        if(params.containsKey("pageId") && null !=params.get("pageId") && StringUtils.isNotEmpty(params.get("pageId").toString())) {
            condition.put("page_id", params.get("pageId"));
        }
        if(params.containsKey("channelId") && null !=params.get("channelId") && StringUtils.isNotEmpty(params.get("channelId").toString())) {
            condition.put("channel_id", params.get("channelId"));
        }
        if(params.containsKey("productId") && null !=params.get("productId") && StringUtils.isNotEmpty(params.get("productId").toString())) {
            condition.put("product_id", params.get("productId"));
        }
        if(params.containsKey("uniqueVisitor") && null !=params.get("uniqueVisitor") && StringUtils.isNotEmpty(params.get("uniqueVisitor").toString())) {
            condition.put("unique_visitor", params.get("uniqueVisitor"));
        }
        if(params.containsKey("pageView") && null !=params.get("pageView") && StringUtils.isNotEmpty(params.get("pageView").toString())) {
            condition.put("page_view", params.get("pageView"));
        }
        if(params.containsKey("dayString") && null !=params.get("dayString") && StringUtils.isNotEmpty(params.get("dayString").toString())) {
            condition.put("day_string", params.get("dayString"));
        }
        if(params.containsKey("createTime") && null !=params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<VisitLogEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        spmMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(SpmMenu.spm_menu);
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
                {"spm_visitlog_table_comment", "访问统计"},
                {"spm_visitlog_column_id", "id"},
                {"spm_visitlog_column_site_id", "网站ID"},
                {"spm_visitlog_column_page_id", "网页ID"},
                {"spm_visitlog_column_channel_id", "频道ID"},
                {"spm_visitlog_column_product_id", "产品ID"},
                {"spm_visitlog_column_unique_visitor", "独立访客(UV)"},
                {"spm_visitlog_column_page_view", "访问量(PV)"},
                {"spm_visitlog_column_day_string", "当天"},
                {"spm_visitlog_column_create_time", "创建时间"},
        };
        return items;
    }

    public String[][] getMenus() {
        String menuKey = SysMenuService.getMenuKey("Spm", "VisitLog");
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"访问统计", "modules/spm/visitlog", "spm:visitlog:list,spm:visitlog:info,spm:visitlog:save,spm:visitlog:update,spm:visitlog:delete", "1", "fa " + ico(), order(), menuKey, parentMenu(), "spm_visitlog_table_comment"},
                {"查看", null, "spm:visitlog:list,spm:visitlog:info", "2", null, order(), SysMenuService.getMenuKey("Spm", "VisitLogInfo"), menuKey, "sys_string_lookup"},
                {"新增", null, "spm:visitlog:save", "2", null, order(), SysMenuService.getMenuKey("Spm", "VisitLogSave"), menuKey, "sys_string_add"},
                {"修改", null, "spm:visitlog:update", "2", null, order(), SysMenuService.getMenuKey("Spm", "VisitLogUpdate"), menuKey, "sys_string_change"},
                {"删除", null, "spm:visitlog:delete", "2", null, order(), SysMenuService.getMenuKey("Spm", "VisitLogDelete"), menuKey, "sys_string_delete"},
        };
        return menus;
    }
}
