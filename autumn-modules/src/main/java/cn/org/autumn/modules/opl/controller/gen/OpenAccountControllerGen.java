package cn.org.autumn.modules.opl.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.opl.entity.OpenAccountEntity;
import cn.org.autumn.modules.opl.service.OpenAccountService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class OpenAccountControllerGen {

    @Autowired
    protected OpenAccountService openAccountService;

    @RequestMapping("/list")
    @RequiresPermissions("opl:openaccount:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = openAccountService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:openaccount:info")
    public R info(@PathVariable("id") Long id) {
        OpenAccountEntity openAccount = openAccountService.selectById(id);
        return R.ok().put("openAccount" , openAccount);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opl:openaccount:save")
    public R save(@RequestBody OpenAccountEntity openAccount) {
        openAccountService.insert(openAccount);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opl:openaccount:update")
    public R update(@RequestBody OpenAccountEntity openAccount) {
        ValidatorUtils.validateEntity(openAccount);
        openAccountService.updateAllColumnById(openAccount);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opl:openaccount:delete")
    public R delete(@RequestBody Long[] ids) {
        openAccountService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
