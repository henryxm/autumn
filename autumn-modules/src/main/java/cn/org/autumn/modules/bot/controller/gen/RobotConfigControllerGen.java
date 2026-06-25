package cn.org.autumn.modules.bot.controller.gen;

import cn.org.autumn.modules.bot.entity.RobotConfigEntity;
import cn.org.autumn.modules.bot.service.RobotConfigService;
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

public class RobotConfigControllerGen {

    @Autowired
    protected RobotConfigService robotConfigService;

    @RequestMapping("/list")
    @RequiresPermissions("bot:robotconfig:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = robotConfigService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("bot:robotconfig:info")
    public R info(@PathVariable("id") Long id) {
        RobotConfigEntity robotConfig = robotConfigService.selectById(id);
        return R.ok().put("robotConfig" , robotConfig);
    }

    @RequestMapping("/save")
    @RequiresPermissions("bot:robotconfig:save")
    public R save(@RequestBody RobotConfigEntity robotConfig) {
        robotConfigService.insert(robotConfig);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("bot:robotconfig:update")
    public R update(@RequestBody RobotConfigEntity robotConfig) {
        ValidatorUtils.validateEntity(robotConfig);
        robotConfigService.updateAllColumnById(robotConfig);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("bot:robotconfig:delete")
    public R delete(@RequestBody Long[] ids) {
        robotConfigService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
