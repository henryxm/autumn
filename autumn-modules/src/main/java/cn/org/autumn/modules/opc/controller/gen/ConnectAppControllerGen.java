package cn.org.autumn.modules.opc.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class ConnectAppControllerGen {

    @Autowired
    protected ConnectAppService connectAppService;

    @RequestMapping("/list")
    @RequiresPermissions("opc:connectapp:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = connectAppService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opc:connectapp:info")
    public R info(@PathVariable("id") Long id) {
        ConnectAppEntity connectApp = connectAppService.selectById(id);
        return R.ok().put("connectApp" , connectApp);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opc:connectapp:save")
    public R save(@RequestBody ConnectAppEntity connectApp) {
        connectAppService.insert(connectApp);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opc:connectapp:update")
    public R update(@RequestBody ConnectAppEntity connectApp) {
        ValidatorUtils.validateEntity(connectApp);
        connectAppService.updateAllColumnById(connectApp);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opc:connectapp:delete")
    public R delete(@RequestBody Long[] ids) {
        connectAppService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
