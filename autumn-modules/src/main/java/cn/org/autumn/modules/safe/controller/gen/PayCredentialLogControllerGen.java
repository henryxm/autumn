package cn.org.autumn.modules.safe.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.safe.entity.PayCredentialLogEntity;
import cn.org.autumn.modules.safe.service.PayCredentialLogService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class PayCredentialLogControllerGen {

    @Autowired
    protected PayCredentialLogService payCredentialLogService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:paycredentiallog:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payCredentialLogService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:paycredentiallog:info")
    public R info(@PathVariable("id") Long id) {
        PayCredentialLogEntity payCredentialLog = payCredentialLogService.selectById(id);
        return R.ok().put("payCredentialLog" , payCredentialLog);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:paycredentiallog:save")
    public R save(@RequestBody PayCredentialLogEntity payCredentialLog) {
        payCredentialLogService.insert(payCredentialLog);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:paycredentiallog:update")
    public R update(@RequestBody PayCredentialLogEntity payCredentialLog) {
        ValidatorUtils.validateEntity(payCredentialLog);
        payCredentialLogService.updateAllColumnById(payCredentialLog);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:paycredentiallog:delete")
    public R delete(@RequestBody Long[] ids) {
        payCredentialLogService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
