package cn.org.autumn.modules.spm.service.gen;

import cn.org.autumn.site.InitFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.spm.service.SpmMenu;
import cn.org.autumn.modules.spm.dao.SuperPositionModelDao;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 超级位置模型控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-07
 */
public class SuperPositionModelServiceGen extends ServiceImpl<SuperPositionModelDao, SuperPositionModelEntity> implements InitFactory.Init {

    @Autowired
    protected SpmMenu spmMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<SuperPositionModelEntity> _page = new Query<SuperPositionModelEntity>(params).getPage();
        QueryWrapper<SuperPositionModelEntity> entityEntityWrapper = new QueryWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("siteId") && null != params.get("siteId") && StringUtils.isNotEmpty(params.get("siteId").toString())) {
            condition.put("site_id", params.get("siteId"));
        }
        if(params.containsKey("pageId") && null != params.get("pageId") && StringUtils.isNotEmpty(params.get("pageId").toString())) {
            condition.put("page_id", params.get("pageId"));
        }
        if(params.containsKey("channelId") && null != params.get("channelId") && StringUtils.isNotEmpty(params.get("channelId").toString())) {
            condition.put("channel_id", params.get("channelId"));
        }
        if(params.containsKey("productId") && null != params.get("productId") && StringUtils.isNotEmpty(params.get("productId").toString())) {
            condition.put("product_id", params.get("productId"));
        }
        if(params.containsKey("resourceId") && null != params.get("resourceId") && StringUtils.isNotEmpty(params.get("resourceId").toString())) {
            condition.put("resource_id", params.get("resourceId"));
        }
        if(params.containsKey("urlPath") && null != params.get("urlPath") && StringUtils.isNotEmpty(params.get("urlPath").toString())) {
            condition.put("url_path", params.get("urlPath"));
        }
        if(params.containsKey("urlKey") && null != params.get("urlKey") && StringUtils.isNotEmpty(params.get("urlKey").toString())) {
            condition.put("url_key", params.get("urlKey"));
        }
        if(params.containsKey("spmValue") && null != params.get("spmValue") && StringUtils.isNotEmpty(params.get("spmValue").toString())) {
            condition.put("spm_value", params.get("spmValue"));
        }
        if(params.containsKey("forbidden") && null != params.get("forbidden") && StringUtils.isNotEmpty(params.get("forbidden").toString())) {
            condition.put("forbidden", params.get("forbidden"));
        }
        if(params.containsKey("needLogin") && null != params.get("needLogin") && StringUtils.isNotEmpty(params.get("needLogin").toString())) {
            condition.put("need_login", params.get("needLogin"));
        }
        Page<SuperPositionModelEntity> page = this.page(_page, entityEntityWrapper);
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
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(spmMenu.getMenu());
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Spm", "SuperPositionModel");
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
                {"spm_superpositionmodel_table_comment", "超级位置模型", "Super Position Model"},
                {"spm_superpositionmodel_column_id", "id", "Id"},
                {"spm_superpositionmodel_column_site_id", "网站ID", "Site Id"},
                {"spm_superpositionmodel_column_page_id", "网页ID", "Page Id"},
                {"spm_superpositionmodel_column_channel_id", "频道ID", "Channel Id"},
                {"spm_superpositionmodel_column_product_id", "产品ID", "Product Id"},
                {"spm_superpositionmodel_column_resource_id", "资源ID", "Resource Id"},
                {"spm_superpositionmodel_column_url_path", "URL路径", "Url Path"},
                {"spm_superpositionmodel_column_url_key", "URLKey", "Url Key"},
                {"spm_superpositionmodel_column_spm_value", "SPM值", "Spm Value"},
                {"spm_superpositionmodel_column_forbidden", "是否禁用", "Forbidden"},
                {"spm_superpositionmodel_column_need_login", "需要登录", "Need Login"},
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
                {"超级位置模型", "modules/spm/superpositionmodel", "spm:superpositionmodel:list,spm:superpositionmodel:info,spm:superpositionmodel:save,spm:superpositionmodel:update,spm:superpositionmodel:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "spm_superpositionmodel_table_comment"},
                {"查看", null, "spm:superpositionmodel:list,spm:superpositionmodel:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "spm:superpositionmodel:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "spm:superpositionmodel:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "spm:superpositionmodel:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
