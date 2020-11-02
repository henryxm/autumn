package cn.org.autumn.modules.spm.service.gen;

import cn.org.autumn.table.TableInit;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.spm.service.SpmMenu;
import cn.org.autumn.modules.spm.dao.VisitLogDao;
import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 访问统计控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class VisitLogServiceGen extends ServiceImpl<VisitLogDao, VisitLogEntity> {

    protected static final String NULL = null;

    @Autowired
    protected SpmMenu spmMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

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
    public int parentMenu(){
        spmMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(SpmMenu.spm_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 45;
    }

    public String ico(){
        return "fa-file-code-o";
    }

    private String order(){
        return String.valueOf(menuOrder());
    }

    private String parent(){
        return String.valueOf(parentMenu());
    }

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, parent(), "访问统计", "modules/spm/visitlog", "spm:visitlog:list,spm:visitlog:info,spm:visitlog:save,spm:visitlog:update,spm:visitlog:delete", "1", "fa " + ico(), order(), "", "spm_visitlog_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "spm:visitlog:list,spm:visitlog:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "spm:visitlog:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "spm:visitlog:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "spm:visitlog:delete", "2", null, order(), "", "sys_string_delete"},
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
        languageService.addLanguageColumnItem("spm_visitlog_table_comment", "访问统计");
        languageService.addLanguageColumnItem("spm_visitlog_column_id", "id");
        languageService.addLanguageColumnItem("spm_visitlog_column_site_id", "网站ID");
        languageService.addLanguageColumnItem("spm_visitlog_column_page_id", "网页ID");
        languageService.addLanguageColumnItem("spm_visitlog_column_channel_id", "频道ID");
        languageService.addLanguageColumnItem("spm_visitlog_column_product_id", "产品ID");
        languageService.addLanguageColumnItem("spm_visitlog_column_unique_visitor", "独立访客(UV)");
        languageService.addLanguageColumnItem("spm_visitlog_column_page_view", "访问量(PV)");
        languageService.addLanguageColumnItem("spm_visitlog_column_day_string", "当天");
        languageService.addLanguageColumnItem("spm_visitlog_column_create_time", "创建时间");
    }
}
