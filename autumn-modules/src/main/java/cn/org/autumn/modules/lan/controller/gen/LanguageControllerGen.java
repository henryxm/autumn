package cn.org.autumn.modules.lan.controller.gen;

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

import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 国家语言
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
public class LanguageControllerGen {

    @Autowired
    protected LanguageService languageService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("lan:language:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = languageService.queryPage(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("lan:language:info")
    public R info(@PathVariable("id") Long id){
        LanguageEntity language = languageService.getById(id);
        return R.ok().put("language", language);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("lan:language:save")
    public R save(@RequestBody LanguageEntity language){
        languageService.save(language);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("lan:language:update")
    public R update(@RequestBody LanguageEntity language){
        ValidatorUtils.validateEntity(language);
        languageService.updateById(language);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("lan:language:delete")
    public R delete(@RequestBody Long[] ids){
        languageService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }

}
