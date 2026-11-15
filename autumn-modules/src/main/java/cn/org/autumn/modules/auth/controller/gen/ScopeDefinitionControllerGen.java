package cn.org.autumn.modules.auth.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.auth.entity.ScopeDefinitionEntity;
import cn.org.autumn.modules.auth.service.ScopeDefinitionService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class ScopeDefinitionControllerGen {

    @Autowired
    protected ScopeDefinitionService scopeDefinitionService;

    @RequestMapping("/list")
    @RequiresPermissions("auth:scopedefinition:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = scopeDefinitionService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("auth:scopedefinition:info")
    public R info(@PathVariable("id") Long id) {
        ScopeDefinitionEntity scopeDefinition = scopeDefinitionService.getById(id);
        return R.ok().put("scopeDefinition" , scopeDefinition);
    }

    @RequestMapping("/save")
    @RequiresPermissions("auth:scopedefinition:save")
    public R save(@RequestBody ScopeDefinitionEntity scopeDefinition) {
        scopeDefinitionService.save(scopeDefinition);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("auth:scopedefinition:update")
    public R update(@RequestBody ScopeDefinitionEntity scopeDefinition) {
        ValidatorUtils.validateEntity(scopeDefinition);
        scopeDefinitionService.updateById(scopeDefinition);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("auth:scopedefinition:delete")
    public R delete(@RequestBody Long[] ids) {
        scopeDefinitionService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
