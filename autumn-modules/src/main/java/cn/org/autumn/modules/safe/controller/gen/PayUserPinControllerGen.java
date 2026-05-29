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
import cn.org.autumn.modules.safe.entity.PayUserPinEntity;
import cn.org.autumn.modules.safe.service.PayUserPinService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class PayUserPinControllerGen {

    @Autowired
    protected PayUserPinService payUserPinService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:payuserpin:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payUserPinService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:payuserpin:info")
    public R info(@PathVariable("id") Long id) {
        PayUserPinEntity payUserPin = payUserPinService.selectById(id);
        return R.ok().put("payUserPin" , payUserPin);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:payuserpin:save")
    public R save(@RequestBody PayUserPinEntity payUserPin) {
        payUserPinService.insert(payUserPin);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:payuserpin:update")
    public R update(@RequestBody PayUserPinEntity payUserPin) {
        ValidatorUtils.validateEntity(payUserPin);
        payUserPinService.updateAllColumnById(payUserPin);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:payuserpin:delete")
    public R delete(@RequestBody Long[] ids) {
        payUserPinService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
