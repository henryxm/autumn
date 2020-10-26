package cn.org.autumn.modules.lan.service.gen;

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

import cn.org.autumn.modules.lan.dao.LanguageDao;
import cn.org.autumn.modules.lan.entity.LanguageEntity;

import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;

/**
 * 国家语言控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

public class LanguageServiceGen extends ServiceImpl<LanguageDao, LanguageEntity> {

    protected static final String NULL = null;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<LanguageEntity> _page = new Query<LanguageEntity>(params).getPage();
        EntityWrapper<LanguageEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("id") && null !=params.get("id") && StringUtils.isNotEmpty(params.get("id").toString())) {
            condition.put("id", params.get("id"));
        }
        if(params.containsKey("name") && null !=params.get("name") && StringUtils.isNotEmpty(params.get("name").toString())) {
            condition.put("name", params.get("name"));
        }
        if(params.containsKey("enUs") && null !=params.get("enUs") && StringUtils.isNotEmpty(params.get("enUs").toString())) {
            condition.put("en_us", params.get("enUs"));
        }
        if(params.containsKey("zhCn") && null !=params.get("zhCn") && StringUtils.isNotEmpty(params.get("zhCn").toString())) {
            condition.put("zh_cn", params.get("zhCn"));
        }
        if(params.containsKey("zhHk") && null !=params.get("zhHk") && StringUtils.isNotEmpty(params.get("zhHk").toString())) {
            condition.put("zh_hk", params.get("zhHk"));
        }
        if(params.containsKey("koKr") && null !=params.get("koKr") && StringUtils.isNotEmpty(params.get("koKr").toString())) {
            condition.put("ko_kr", params.get("koKr"));
        }
        if(params.containsKey("jaJp") && null !=params.get("jaJp") && StringUtils.isNotEmpty(params.get("jaJp").toString())) {
            condition.put("ja_jp", params.get("jaJp"));
        }
        if(params.containsKey("ttRu") && null !=params.get("ttRu") && StringUtils.isNotEmpty(params.get("ttRu").toString())) {
            condition.put("tt_ru", params.get("ttRu"));
        }
        if(params.containsKey("frFr") && null !=params.get("frFr") && StringUtils.isNotEmpty(params.get("frFr").toString())) {
            condition.put("fr_fr", params.get("frFr"));
        }
        if(params.containsKey("deDe") && null !=params.get("deDe") && StringUtils.isNotEmpty(params.get("deDe").toString())) {
            condition.put("de_de", params.get("deDe"));
        }
        if(params.containsKey("viVn") && null !=params.get("viVn") && StringUtils.isNotEmpty(params.get("viVn").toString())) {
            condition.put("vi_vn", params.get("viVn"));
        }
        if(params.containsKey("thTh") && null !=params.get("thTh") && StringUtils.isNotEmpty(params.get("thTh").toString())) {
            condition.put("th_th", params.get("thTh"));
        }
        if(params.containsKey("msMy") && null !=params.get("msMy") && StringUtils.isNotEmpty(params.get("msMy").toString())) {
            condition.put("ms_my", params.get("msMy"));
        }
        if(params.containsKey("idId") && null !=params.get("idId") && StringUtils.isNotEmpty(params.get("idId").toString())) {
            condition.put("id_id", params.get("idId"));
        }
        if(params.containsKey("esEs") && null !=params.get("esEs") && StringUtils.isNotEmpty(params.get("esEs").toString())) {
            condition.put("es_es", params.get("esEs"));
        }
        if(params.containsKey("trTr") && null !=params.get("trTr") && StringUtils.isNotEmpty(params.get("trTr").toString())) {
            condition.put("tr_tr", params.get("trTr"));
        }
        if(params.containsKey("ukUk") && null !=params.get("ukUk") && StringUtils.isNotEmpty(params.get("ukUk").toString())) {
            condition.put("uk_uk", params.get("ukUk"));
        }
        if(params.containsKey("puPt") && null !=params.get("puPt") && StringUtils.isNotEmpty(params.get("puPt").toString())) {
            condition.put("pu_pt", params.get("puPt"));
        }
        if(params.containsKey("plPl") && null !=params.get("plPl") && StringUtils.isNotEmpty(params.get("plPl").toString())) {
            condition.put("pl_pl", params.get("plPl"));
        }
        if(params.containsKey("mnMn") && null !=params.get("mnMn") && StringUtils.isNotEmpty(params.get("mnMn").toString())) {
            condition.put("mn_mn", params.get("mnMn"));
        }
        if(params.containsKey("nbNo") && null !=params.get("nbNo") && StringUtils.isNotEmpty(params.get("nbNo").toString())) {
            condition.put("nb_no", params.get("nbNo"));
        }
        if(params.containsKey("itIt") && null !=params.get("itIt") && StringUtils.isNotEmpty(params.get("itIt").toString())) {
            condition.put("it_it", params.get("itIt"));
        }
        if(params.containsKey("heIl") && null !=params.get("heIl") && StringUtils.isNotEmpty(params.get("heIl").toString())) {
            condition.put("he_il", params.get("heIl"));
        }
        if(params.containsKey("elGr") && null !=params.get("elGr") && StringUtils.isNotEmpty(params.get("elGr").toString())) {
            condition.put("el_gr", params.get("elGr"));
        }
        if(params.containsKey("faIr") && null !=params.get("faIr") && StringUtils.isNotEmpty(params.get("faIr").toString())) {
            condition.put("fa_ir", params.get("faIr"));
        }
        if(params.containsKey("arSa") && null !=params.get("arSa") && StringUtils.isNotEmpty(params.get("arSa").toString())) {
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
    public int parentMenu(){
        return 1;
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
                {null, parent(), "国家语言", "modules/lan/language.html", "lan:language:list,lan:language:info,lan:language:save,lan:language:update,lan:language:delete", "1", "fa " + ico(), order()};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "lan:language:list,lan:language:info", "2", null, order()},
                {null, id + "", "新增", null, "lan:language:save", "2", null, order()},
                {null, id + "", "修改", null, "lan:language:update", "2", null, order()},
                {null, id + "", "删除", null, "lan:language:delete", "2", null, order()},
        };
        for (String[] menu : menus) {
            sysMenu = sysMenuService.from(menu);
            entity = sysMenuService.get(sysMenu);
            if (null == entity) {
                sysMenuService.put(sysMenu);
            }
        }
    }
}
