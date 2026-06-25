package cn.org.autumn.modules.safe.controller.gen;

import cn.org.autumn.modules.safe.entity.PayUserGestureEntity;
import cn.org.autumn.modules.safe.service.PayUserGestureService;
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

public class PayUserGestureControllerGen {

    @Autowired
    protected PayUserGestureService payUserGestureService;

    @RequestMapping("/list")
    @RequiresPermissions("safe:payusergesture:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = payUserGestureService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("safe:payusergesture:info")
    public R info(@PathVariable("id") Long id) {
        PayUserGestureEntity payUserGesture = payUserGestureService.selectById(id);
        return R.ok().put("payUserGesture" , payUserGesture);
    }

    @RequestMapping("/save")
    @RequiresPermissions("safe:payusergesture:save")
    public R save(@RequestBody PayUserGestureEntity payUserGesture) {
        payUserGestureService.insert(payUserGesture);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("safe:payusergesture:update")
    public R update(@RequestBody PayUserGestureEntity payUserGesture) {
        ValidatorUtils.validateEntity(payUserGesture);
        payUserGestureService.updateAllColumnById(payUserGesture);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("safe:payusergesture:delete")
    public R delete(@RequestBody Long[] ids) {
        payUserGestureService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
