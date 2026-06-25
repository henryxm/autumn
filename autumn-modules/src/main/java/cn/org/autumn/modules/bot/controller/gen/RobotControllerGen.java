package cn.org.autumn.modules.bot.controller.gen;

import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.RobotService;
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

public class RobotControllerGen {

    @Autowired
    protected RobotService robotService;

    @RequestMapping("/list")
    @RequiresPermissions("bot:robot:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = robotService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("bot:robot:info")
    public R info(@PathVariable("id") Long id) {
        RobotEntity robot = robotService.selectById(id);
        return R.ok().put("robot" , robot);
    }

    @RequestMapping("/save")
    @RequiresPermissions("bot:robot:save")
    public R save(@RequestBody RobotEntity robot) {
        robotService.insert(robot);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("bot:robot:update")
    public R update(@RequestBody RobotEntity robot) {
        ValidatorUtils.validateEntity(robot);
        robotService.updateAllColumnById(robot);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("bot:robot:delete")
    public R delete(@RequestBody Long[] ids) {
        robotService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
