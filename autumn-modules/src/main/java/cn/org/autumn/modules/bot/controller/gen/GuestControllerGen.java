package cn.org.autumn.modules.bot.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.bot.entity.GuestEntity;
import cn.org.autumn.modules.bot.service.GuestService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class GuestControllerGen {

    @Autowired
    protected GuestService guestService;

    @RequestMapping("/list")
    @RequiresPermissions("bot:guest:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = guestService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("bot:guest:info")
    public R info(@PathVariable("id") Long id) {
        GuestEntity guest = guestService.getById(id);
        return R.ok().put("guest" , guest);
    }

    @RequestMapping("/save")
    @RequiresPermissions("bot:guest:save")
    public R save(@RequestBody GuestEntity guest) {
        guestService.save(guest);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("bot:guest:update")
    public R update(@RequestBody GuestEntity guest) {
        ValidatorUtils.validateEntity(guest);
        guestService.updateById(guest);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("bot:guest:delete")
    public R delete(@RequestBody Long[] ids) {
        guestService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
