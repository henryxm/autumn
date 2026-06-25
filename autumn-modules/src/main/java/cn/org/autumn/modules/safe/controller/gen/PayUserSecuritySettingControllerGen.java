package cn.org.autumn.modules.safe.controller.gen;

import cn.org.autumn.modules.safe.entity.PayUserSecuritySettingEntity;
import cn.org.autumn.modules.safe.service.PayUserSecuritySettingService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import java.util.Arrays;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class PayUserSecuritySettingControllerGen {

    @Autowired
    protected PayUserSecuritySettingService payUserSecuritySettingService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:payusersecuritysetting:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payUserSecuritySettingService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:payusersecuritysetting:info")
    public R info(@PathVariable("id") Long id) {
        PayUserSecuritySettingEntity payUserSecuritySetting = payUserSecuritySettingService.selectById(id);
        return R.ok().put("payUserSecuritySetting" , payUserSecuritySetting);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:payusersecuritysetting:save")
    public R save(@RequestBody PayUserSecuritySettingEntity payUserSecuritySetting) {
        payUserSecuritySettingService.insert(payUserSecuritySetting);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:payusersecuritysetting:update")
    public R update(@RequestBody PayUserSecuritySettingEntity payUserSecuritySetting) {
        ValidatorUtils.validateEntity(payUserSecuritySetting);
        payUserSecuritySettingService.updateAllColumnById(payUserSecuritySetting);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:payusersecuritysetting:delete")
    public R delete(@RequestBody Long[] ids) {
        payUserSecuritySettingService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
