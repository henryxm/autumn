package cn.org.autumn.modules.test.service.gen;

import cn.org.autumn.table.TableInit;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;

import cn.org.autumn.modules.test.service.TestMenu;
import cn.org.autumn.modules.test.dao.DemoExampleDao;
import cn.org.autumn.modules.test.entity.DemoExampleEntity;

import javax.annotation.PostConstruct;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;

/**
 * 例子控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

public class DemoExampleServiceGen extends ServiceImpl<DemoExampleDao, DemoExampleEntity> {

    protected static final String NULL = null;

    @Autowired
    protected TestMenu testMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

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
    public int parentMenu(){
        testMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(TestMenu.test_menu);
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 48;
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
                {null, parent(), "例子", "modules/test/demoexample.html", "test:demoexample:list,test:demoexample:info,test:demoexample:save,test:demoexample:update,test:demoexample:delete", "1", "fa " + ico(), order()};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "test:demoexample:list,test:demoexample:info", "2", null, order()},
                {null, id + "", "新增", null, "test:demoexample:save", "2", null, order()},
                {null, id + "", "修改", null, "test:demoexample:update", "2", null, order()},
                {null, id + "", "删除", null, "test:demoexample:delete", "2", null, order()},
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
