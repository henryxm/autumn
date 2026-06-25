package cn.org.autumn.modules.safe.controller.gen;

import cn.org.autumn.modules.safe.entity.PayUserBiometricEntity;
import cn.org.autumn.modules.safe.service.PayUserBiometricService;
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

public class PayUserBiometricControllerGen {

    @Autowired
    protected PayUserBiometricService payUserBiometricService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:payuserbiometric:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payUserBiometricService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:payuserbiometric:info")
    public R info(@PathVariable("id") Long id) {
        PayUserBiometricEntity payUserBiometric = payUserBiometricService.selectById(id);
        return R.ok().put("payUserBiometric" , payUserBiometric);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:payuserbiometric:save")
    public R save(@RequestBody PayUserBiometricEntity payUserBiometric) {
        payUserBiometricService.insert(payUserBiometric);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:payuserbiometric:update")
    public R update(@RequestBody PayUserBiometricEntity payUserBiometric) {
        ValidatorUtils.validateEntity(payUserBiometric);
        payUserBiometricService.updateAllColumnById(payUserBiometric);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:payuserbiometric:delete")
    public R delete(@RequestBody Long[] ids) {
        payUserBiometricService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
