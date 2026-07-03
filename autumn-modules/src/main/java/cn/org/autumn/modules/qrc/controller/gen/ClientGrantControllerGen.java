package cn.org.autumn.modules.qrc.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class ClientGrantControllerGen {

    @Autowired
    protected ClientGrantService clientGrantService;

    @RequestMapping("/list")
    @RequiresPermissions("qrc:clientgrant:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = clientGrantService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("qrc:clientgrant:info")
    public R info(@PathVariable("id") Long id) {
        ClientGrantEntity clientGrant = clientGrantService.selectById(id);
        return R.ok().put("clientGrant" , clientGrant);
    }

    @RequestMapping("/save")
    @RequiresPermissions("qrc:clientgrant:save")
    public R save(@RequestBody ClientGrantEntity clientGrant) {
        clientGrantService.insert(clientGrant);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("qrc:clientgrant:update")
    public R update(@RequestBody ClientGrantEntity clientGrant) {
        ValidatorUtils.validateEntity(clientGrant);
        clientGrantService.updateAllColumnById(clientGrant);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("qrc:clientgrant:delete")
    public R delete(@RequestBody Long[] ids) {
        clientGrantService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
