package cn.org.autumn.modules.test.controller.gen;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.test.entity.DemoExampleEntity;
import cn.org.autumn.modules.test.service.DemoExampleService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 例子
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
public class DemoExampleControllerGen {

    @Autowired
    protected DemoExampleService demoExampleService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:demoexample:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = demoExampleService.queryPage(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("test:demoexample:info")
    public R info(@PathVariable("id") Long id){
        DemoExampleEntity demoExample = demoExampleService.selectById(id);
        return R.ok().put("demoExample", demoExample);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("test:demoexample:save")
    public R save(@RequestBody DemoExampleEntity demoExample){
        demoExampleService.insert(demoExample);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("test:demoexample:update")
    public R update(@RequestBody DemoExampleEntity demoExample){
        ValidatorUtils.validateEntity(demoExample);
        demoExampleService.updateAllColumnById(demoExample);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("test:demoexample:delete")
    public R delete(@RequestBody Long[] ids){
        demoExampleService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }

}
