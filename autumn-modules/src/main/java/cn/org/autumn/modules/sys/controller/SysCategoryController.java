package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.entity.SysCategoryEntity;
import cn.org.autumn.modules.sys.service.SysCategoryService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 系统配置类型表
 *
 * @author User
 * @email henryxm@163.com
 * @date 2022-12
 */
@RestController
@RequestMapping("sys/category")
public class SysCategoryController {

    @Autowired
    protected SysCategoryService sysCategoryService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:category:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysCategoryService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sys:category:info")
    public R info(@PathVariable("id") Long id) {
        SysCategoryEntity category = sysCategoryService.getById(id);
        return R.ok().put("category" , category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sys:category:save")
    public R save(@RequestBody SysCategoryEntity category) {
        sysCategoryService.save(category);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sys:category:update")
    public R update(@RequestBody SysCategoryEntity category) {
        ValidatorUtils.validateEntity(category);
        sysCategoryService.updateById(category);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sys:category:delete")
    public R delete(@RequestBody Long[] ids) {
        sysCategoryService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
