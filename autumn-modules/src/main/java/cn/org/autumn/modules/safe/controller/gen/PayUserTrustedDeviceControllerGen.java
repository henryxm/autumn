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
import cn.org.autumn.modules.safe.entity.PayUserTrustedDeviceEntity;
import cn.org.autumn.modules.safe.service.PayUserTrustedDeviceService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class PayUserTrustedDeviceControllerGen {

    @Autowired
    protected PayUserTrustedDeviceService payUserTrustedDeviceService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:payusertrusteddevice:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payUserTrustedDeviceService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:payusertrusteddevice:info")
    public R info(@PathVariable("id") Long id) {
        PayUserTrustedDeviceEntity payUserTrustedDevice = payUserTrustedDeviceService.selectById(id);
        return R.ok().put("payUserTrustedDevice" , payUserTrustedDevice);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:payusertrusteddevice:save")
    public R save(@RequestBody PayUserTrustedDeviceEntity payUserTrustedDevice) {
        payUserTrustedDeviceService.insert(payUserTrustedDevice);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:payusertrusteddevice:update")
    public R update(@RequestBody PayUserTrustedDeviceEntity payUserTrustedDevice) {
        ValidatorUtils.validateEntity(payUserTrustedDevice);
        payUserTrustedDeviceService.updateAllColumnById(payUserTrustedDevice);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:payusertrusteddevice:delete")
    public R delete(@RequestBody Long[] ids) {
        payUserTrustedDeviceService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
