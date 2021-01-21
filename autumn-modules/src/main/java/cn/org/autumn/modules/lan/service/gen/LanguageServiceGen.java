package cn.org.autumn.modules.lan.service.gen;

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
import cn.org.autumn.modules.lan.service.LanMenu;
import cn.org.autumn.modules.lan.dao.LanguageDao;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 国家语言控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class LanguageServiceGen extends ServiceImpl<LanguageDao, LanguageEntity> implements InitFactory.Init {

    @Autowired
    protected LanMenu lanMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<LanguageEntity> _page = new Query<LanguageEntity>(params).getPage();
        EntityWrapper<LanguageEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null != params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("name") && null != params.get("name") && StringUtils.isNotEmpty(params.get("name").toString())) {
            condition.put("name", params.get("name"));
        }
        if(params.containsKey("zhCn") && null != params.get("zhCn") && StringUtils.isNotEmpty(params.get("zhCn").toString())) {
            condition.put("zh_cn", params.get("zhCn"));
        }
        if(params.containsKey("enUs") && null != params.get("enUs") && StringUtils.isNotEmpty(params.get("enUs").toString())) {
            condition.put("en_us", params.get("enUs"));
        }
        if(params.containsKey("zhHk") && null != params.get("zhHk") && StringUtils.isNotEmpty(params.get("zhHk").toString())) {
            condition.put("zh_hk", params.get("zhHk"));
        }
        if(params.containsKey("koKr") && null != params.get("koKr") && StringUtils.isNotEmpty(params.get("koKr").toString())) {
            condition.put("ko_kr", params.get("koKr"));
        }
        if(params.containsKey("jaJp") && null != params.get("jaJp") && StringUtils.isNotEmpty(params.get("jaJp").toString())) {
            condition.put("ja_jp", params.get("jaJp"));
        }
        if(params.containsKey("ttRu") && null != params.get("ttRu") && StringUtils.isNotEmpty(params.get("ttRu").toString())) {
            condition.put("tt_ru", params.get("ttRu"));
        }
        if(params.containsKey("frFr") && null != params.get("frFr") && StringUtils.isNotEmpty(params.get("frFr").toString())) {
            condition.put("fr_fr", params.get("frFr"));
        }
        if(params.containsKey("deDe") && null != params.get("deDe") && StringUtils.isNotEmpty(params.get("deDe").toString())) {
            condition.put("de_de", params.get("deDe"));
        }
        if(params.containsKey("viVn") && null != params.get("viVn") && StringUtils.isNotEmpty(params.get("viVn").toString())) {
            condition.put("vi_vn", params.get("viVn"));
        }
        if(params.containsKey("thTh") && null != params.get("thTh") && StringUtils.isNotEmpty(params.get("thTh").toString())) {
            condition.put("th_th", params.get("thTh"));
        }
        if(params.containsKey("msMy") && null != params.get("msMy") && StringUtils.isNotEmpty(params.get("msMy").toString())) {
            condition.put("ms_my", params.get("msMy"));
        }
        if(params.containsKey("idId") && null != params.get("idId") && StringUtils.isNotEmpty(params.get("idId").toString())) {
            condition.put("id_id", params.get("idId"));
        }
        if(params.containsKey("esEs") && null != params.get("esEs") && StringUtils.isNotEmpty(params.get("esEs").toString())) {
            condition.put("es_es", params.get("esEs"));
        }
        if(params.containsKey("trTr") && null != params.get("trTr") && StringUtils.isNotEmpty(params.get("trTr").toString())) {
            condition.put("tr_tr", params.get("trTr"));
        }
        if(params.containsKey("ukUk") && null != params.get("ukUk") && StringUtils.isNotEmpty(params.get("ukUk").toString())) {
            condition.put("uk_uk", params.get("ukUk"));
        }
        if(params.containsKey("puPt") && null != params.get("puPt") && StringUtils.isNotEmpty(params.get("puPt").toString())) {
            condition.put("pu_pt", params.get("puPt"));
        }
        if(params.containsKey("plPl") && null != params.get("plPl") && StringUtils.isNotEmpty(params.get("plPl").toString())) {
            condition.put("pl_pl", params.get("plPl"));
        }
        if(params.containsKey("mnMn") && null != params.get("mnMn") && StringUtils.isNotEmpty(params.get("mnMn").toString())) {
            condition.put("mn_mn", params.get("mnMn"));
        }
        if(params.containsKey("nbNo") && null != params.get("nbNo") && StringUtils.isNotEmpty(params.get("nbNo").toString())) {
            condition.put("nb_no", params.get("nbNo"));
        }
        if(params.containsKey("itIt") && null != params.get("itIt") && StringUtils.isNotEmpty(params.get("itIt").toString())) {
            condition.put("it_it", params.get("itIt"));
        }
        if(params.containsKey("heIl") && null != params.get("heIl") && StringUtils.isNotEmpty(params.get("heIl").toString())) {
            condition.put("he_il", params.get("heIl"));
        }
        if(params.containsKey("elGr") && null != params.get("elGr") && StringUtils.isNotEmpty(params.get("elGr").toString())) {
            condition.put("el_gr", params.get("elGr"));
        }
        if(params.containsKey("faIr") && null != params.get("faIr") && StringUtils.isNotEmpty(params.get("faIr").toString())) {
            condition.put("fa_ir", params.get("faIr"));
        }
        if(params.containsKey("arSa") && null != params.get("arSa") && StringUtils.isNotEmpty(params.get("arSa").toString())) {
            condition.put("ar_sa", params.get("arSa"));
        }
        _page.setCondition(condition);
        Page<LanguageEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        lanMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(lanMenu.getMenu());
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Lan", "Language");
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
                {"lan_language_table_comment", "国家语言"},
                {"lan_language_column_id", "id"},
                {"lan_language_column_name", "标识"},
                {"lan_language_column_zh_cn", "简体中文(中国)"},
                {"lan_language_column_en_us", "英语(美国)"},
                {"lan_language_column_zh_hk", "繁体中文(香港)"},
                {"lan_language_column_ko_kr", "韩语(韩国)"},
                {"lan_language_column_ja_jp", "日语(日本)"},
                {"lan_language_column_tt_ru", "俄语(俄罗斯)"},
                {"lan_language_column_fr_fr", "法语(法国)"},
                {"lan_language_column_de_de", "德语(德国)"},
                {"lan_language_column_vi_vn", "越语(越南)"},
                {"lan_language_column_th_th", "泰语(泰国)"},
                {"lan_language_column_ms_my", "马来语(马来西亚)"},
                {"lan_language_column_id_id", "印尼语(印尼)"},
                {"lan_language_column_es_es", "西班牙语(西班牙)"},
                {"lan_language_column_tr_tr", "土耳其语(土耳其)"},
                {"lan_language_column_uk_uk", "乌克兰语(乌克兰)"},
                {"lan_language_column_pu_pt", "葡萄牙语(葡萄牙)"},
                {"lan_language_column_pl_pl", "波兰语(波兰)"},
                {"lan_language_column_mn_mn", "蒙古语(蒙古)"},
                {"lan_language_column_nb_no", "挪威语(挪威)"},
                {"lan_language_column_it_it", "意大利语(意大利)"},
                {"lan_language_column_he_il", "希伯来语(以色列)"},
                {"lan_language_column_el_gr", "希腊语(希腊)"},
                {"lan_language_column_fa_ir", "波斯语(伊朗)"},
                {"lan_language_column_ar_sa", "阿拉伯语(沙特阿拉伯)"},
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
                {"国家语言", "modules/lan/language", "lan:language:list,lan:language:info,lan:language:save,lan:language:update,lan:language:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "lan_language_table_comment"},
                {"查看", null, "lan:language:list,lan:language:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "lan:language:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "lan:language:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "lan:language:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
