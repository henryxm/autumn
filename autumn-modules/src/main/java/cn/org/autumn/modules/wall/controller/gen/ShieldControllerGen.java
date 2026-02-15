package cn.org.autumn.modules.wall.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.wall.entity.ShieldEntity;
import cn.org.autumn.modules.wall.service.ShieldService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 攻击防御
 *
 * @author User
 * @email henryxm@163.com
 * @date 2024-11
 */
public class ShieldControllerGen {

    @Autowired
    protected ShieldService shieldService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:shield:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = shieldService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:shield:info")
    public R info(@PathVariable("id") Long id) {
        ShieldEntity shield = shieldService.getById(id);
        return R.ok().put("shield" , shield);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:shield:save")
    public R save(@RequestBody ShieldEntity shield) {
        shieldService.save(shield);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:shield:update")
    public R update(@RequestBody ShieldEntity shield) {
        ValidatorUtils.validateEntity(shield);
        shieldService.updateById(shield);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:shield:delete")
    public R delete(@RequestBody Long[] ids) {
        shieldService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
