package cn.org.autumn.modules.safe.controller.gen;

import cn.org.autumn.modules.safe.entity.PayUserTrustedIpEntity;
import cn.org.autumn.modules.safe.service.PayUserTrustedIpService;
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

public class PayUserTrustedIpControllerGen {

    @Autowired
    protected PayUserTrustedIpService payUserTrustedIpService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:payusertrustedip:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payUserTrustedIpService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:payusertrustedip:info")
    public R info(@PathVariable("id") Long id) {
        PayUserTrustedIpEntity payUserTrustedIp = payUserTrustedIpService.selectById(id);
        return R.ok().put("payUserTrustedIp" , payUserTrustedIp);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:payusertrustedip:save")
    public R save(@RequestBody PayUserTrustedIpEntity payUserTrustedIp) {
        payUserTrustedIpService.insert(payUserTrustedIp);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:payusertrustedip:update")
    public R update(@RequestBody PayUserTrustedIpEntity payUserTrustedIp) {
        ValidatorUtils.validateEntity(payUserTrustedIp);
        payUserTrustedIpService.updateAllColumnById(payUserTrustedIp);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:payusertrustedip:delete")
    public R delete(@RequestBody Long[] ids) {
        payUserTrustedIpService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
