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
import cn.org.autumn.modules.wall.entity.JumpEntity;
import cn.org.autumn.modules.wall.service.JumpService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 攻击跳转
 *
 * @author User
 * @email henryxm@163.com
 * @date 2024-11
 */
public class JumpControllerGen {

    @Autowired
    protected JumpService jumpService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:jump:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = jumpService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:jump:info")
    public R info(@PathVariable("id") Long id) {
        JumpEntity jump = jumpService.getById(id);
        return R.ok().put("jump" , jump);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:jump:save")
    public R save(@RequestBody JumpEntity jump) {
        jumpService.save(jump);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:jump:update")
    public R update(@RequestBody JumpEntity jump) {
        ValidatorUtils.validateEntity(jump);
        jumpService.updateById(jump);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:jump:delete")
    public R delete(@RequestBody Long[] ids) {
        jumpService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
