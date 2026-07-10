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
    @RequiresPermissions("auth:scopedef:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = scopeDefinitionService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("auth:scopedef:info")
    public R info(@PathVariable("id") Long id) {
        ScopeDefinitionEntity scopeDef = scopeDefinitionService.selectById(id);
        return R.ok().put("scopeDef" , scopeDef);
    }

    @RequestMapping("/save")
    @RequiresPermissions("auth:scopedef:save")
    public R save(@RequestBody ScopeDefinitionEntity scopeDef) {
        scopeDefinitionService.insert(scopeDef);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("auth:scopedef:update")
    public R update(@RequestBody ScopeDefinitionEntity scopeDef) {
        ValidatorUtils.validateEntity(scopeDef);
        scopeDefinitionService.updateAllColumnById(scopeDef);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("auth:scopedef:delete")
    public R delete(@RequestBody Long[] ids) {
        scopeDefinitionService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
