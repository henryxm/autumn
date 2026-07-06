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
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.opc.service.ConnectBindService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class ConnectBindControllerGen {

    @Autowired
    protected ConnectBindService connectBindService;

    @RequestMapping("/list")
    @RequiresPermissions("opc:connectbind:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = connectBindService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opc:connectbind:info")
    public R info(@PathVariable("id") Long id) {
        ConnectBindEntity connectBind = connectBindService.selectById(id);
        return R.ok().put("connectBind" , connectBind);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opc:connectbind:save")
    public R save(@RequestBody ConnectBindEntity connectBind) {
        connectBindService.insert(connectBind);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opc:connectbind:update")
    public R update(@RequestBody ConnectBindEntity connectBind) {
        ValidatorUtils.validateEntity(connectBind);
        connectBindService.updateAllColumnById(connectBind);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opc:connectbind:delete")
    public R delete(@RequestBody Long[] ids) {
        connectBindService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
