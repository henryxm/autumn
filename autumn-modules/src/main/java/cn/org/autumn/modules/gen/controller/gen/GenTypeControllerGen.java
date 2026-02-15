package cn.org.autumn.modules.gen.controller.gen;

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

import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.gen.service.GenTypeService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 生成方案
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
public class GenTypeControllerGen {

    @Autowired
    protected GenTypeService genTypeService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("gen:gentype:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = genTypeService.queryPage(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("gen:gentype:info")
    public R info(@PathVariable("id") Long id){
        GenTypeEntity genType = genTypeService.getById(id);
        return R.ok().put("genType", genType);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("gen:gentype:save")
    public R save(@RequestBody GenTypeEntity genType){
        genTypeService.save(genType);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("gen:gentype:update")
    public R update(@RequestBody GenTypeEntity genType){
        ValidatorUtils.validateEntity(genType);
        genTypeService.updateById(genType);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("gen:gentype:delete")
    public R delete(@RequestBody Long[] ids){
        genTypeService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }

    @RequestMapping("/copy")
    @RequiresPermissions("gen:gentype:copy")
    public R copy(@RequestBody Long[] ids){
        genTypeService.copy(ids);
        return R.ok();
    }
}
