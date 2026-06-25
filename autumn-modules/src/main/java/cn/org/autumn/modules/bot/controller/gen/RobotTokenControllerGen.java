package cn.org.autumn.modules.bot.controller.gen;

import cn.org.autumn.modules.bot.entity.RobotTokenEntity;
import cn.org.autumn.modules.bot.service.RobotTokenService;
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

public class RobotTokenControllerGen {

    @Autowired
    protected RobotTokenService robotTokenService;

    @RequestMapping("/list")
    @RequiresPermissions("bot:robottoken:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = robotTokenService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("bot:robottoken:info")
    public R info(@PathVariable("id") Long id) {
        RobotTokenEntity robotToken = robotTokenService.selectById(id);
        return R.ok().put("robotToken" , robotToken);
    }

    @RequestMapping("/save")
    @RequiresPermissions("bot:robottoken:save")
    public R save(@RequestBody RobotTokenEntity robotToken) {
        robotTokenService.insert(robotToken);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("bot:robottoken:update")
    public R update(@RequestBody RobotTokenEntity robotToken) {
        ValidatorUtils.validateEntity(robotToken);
        robotTokenService.updateAllColumnById(robotToken);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("bot:robottoken:delete")
    public R delete(@RequestBody Long[] ids) {
        robotTokenService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
