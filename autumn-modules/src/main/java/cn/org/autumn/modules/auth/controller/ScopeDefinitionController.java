package cn.org.autumn.modules.auth.controller;

import cn.org.autumn.modules.auth.controller.gen.ScopeDefinitionControllerGen;
import cn.org.autumn.modules.auth.entity.ScopeDefinitionEntity;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import java.util.Arrays;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * gen CRUD 扩展：路径与 {@code scopedefinition.js} 对齐；写操作捕获业务校验异常。
 */
@RestController
@RequestMapping("auth/scopedefinition")
public class ScopeDefinitionController extends ScopeDefinitionControllerGen {

    @Override
    @RequestMapping("/save")
    @RequiresPermissions("auth:scopedefinition:save")
    public R save(@RequestBody ScopeDefinitionEntity scopeDefinition) {
        try {
            scopeDefinitionService.save(scopeDefinition);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }

    @Override
    @RequestMapping("/update")
    @RequiresPermissions("auth:scopedefinition:update")
    public R update(@RequestBody ScopeDefinitionEntity scopeDefinition) {
        try {
            ValidatorUtils.validateEntity(scopeDefinition);
            scopeDefinitionService.updateById(scopeDefinition);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }

    @Override
    @RequestMapping("/delete")
    @RequiresPermissions("auth:scopedefinition:delete")
    public R delete(@RequestBody Long[] ids) {
        try {
            scopeDefinitionService.removeByIds(Arrays.asList(ids));
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }
}
