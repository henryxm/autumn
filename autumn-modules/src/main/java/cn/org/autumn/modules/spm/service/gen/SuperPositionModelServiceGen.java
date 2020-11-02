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
import cn.org.autumn.modules.spm.dao.SuperPositionModelDao;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 超级位置模型控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class SuperPositionModelServiceGen extends ServiceImpl<SuperPositionModelDao, SuperPositionModelEntity> {

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
        Page<SuperPositionModelEntity> _page = new Query<SuperPositionModelEntity>(params).getPage();
        EntityWrapper<SuperPositionModelEntity> entityEntityWrapper = new EntityWrapper<>();
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
        if(params.containsKey("resourceId") && null !=params.get("resourceId") && StringUtils.isNotEmpty(params.get("resourceId").toString())) {
            condition.put("resource_id", params.get("resourceId"));
        }
        if(params.containsKey("urlPath") && null !=params.get("urlPath") && StringUtils.isNotEmpty(params.get("urlPath").toString())) {
            condition.put("url_path", params.get("urlPath"));
        }
        if(params.containsKey("urlKey") && null !=params.get("urlKey") && StringUtils.isNotEmpty(params.get("urlKey").toString())) {
            condition.put("url_key", params.get("urlKey"));
        }
        if(params.containsKey("spmValue") && null !=params.get("spmValue") && StringUtils.isNotEmpty(params.get("spmValue").toString())) {
            condition.put("spm_value", params.get("spmValue"));
        }
        if(params.containsKey("forbidden") && null !=params.get("forbidden") && StringUtils.isNotEmpty(params.get("forbidden").toString())) {
            condition.put("forbidden", params.get("forbidden"));
        }
        if(params.containsKey("needLogin") && null !=params.get("needLogin") && StringUtils.isNotEmpty(params.get("needLogin").toString())) {
            condition.put("need_login", params.get("needLogin"));
        }
        _page.setCondition(condition);
        Page<SuperPositionModelEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        return 69;
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
                {null, parent(), "超级位置模型", "modules/spm/superpositionmodel", "spm:superpositionmodel:list,spm:superpositionmodel:info,spm:superpositionmodel:save,spm:superpositionmodel:update,spm:superpositionmodel:delete", "1", "fa " + ico(), order(), "", "spm_superpositionmodel_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "spm:superpositionmodel:list,spm:superpositionmodel:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "spm:superpositionmodel:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "spm:superpositionmodel:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "spm:superpositionmodel:delete", "2", null, order(), "", "sys_string_delete"},
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
        languageService.addLanguageColumnItem("spm_superpositionmodel_table_comment", "超级位置模型");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_id", "id");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_site_id", "网站ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_page_id", "网页ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_channel_id", "频道ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_product_id", "产品ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_resource_id", "资源ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_url_path", "URL路径");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_url_key", "URLKey");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_spm_value", "SPM值");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_forbidden", "是否禁用");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_need_login", "需要登录");
    }
}
