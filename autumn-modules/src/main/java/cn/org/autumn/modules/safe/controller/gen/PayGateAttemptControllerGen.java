package cn.org.autumn.modules.safe.controller.gen;

import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.service.PayGateAttemptService;
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

public class PayGateAttemptControllerGen {

    @Autowired
    protected PayGateAttemptService payGateAttemptService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:paygateattempt:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payGateAttemptService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:paygateattempt:info")
    public R info(@PathVariable("id") Long id) {
        PayGateAttemptEntity payGateAttempt = payGateAttemptService.selectById(id);
        return R.ok().put("payGateAttempt" , payGateAttempt);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:paygateattempt:save")
    public R save(@RequestBody PayGateAttemptEntity payGateAttempt) {
        payGateAttemptService.insert(payGateAttempt);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:paygateattempt:update")
    public R update(@RequestBody PayGateAttemptEntity payGateAttempt) {
        ValidatorUtils.validateEntity(payGateAttempt);
        payGateAttemptService.updateAllColumnById(payGateAttempt);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:paygateattempt:delete")
    public R delete(@RequestBody Long[] ids) {
        payGateAttemptService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
