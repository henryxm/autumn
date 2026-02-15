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
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * IP黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class IpBlackControllerGen {

    @Autowired
    protected IpBlackService ipBlackService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:ipblack:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = ipBlackService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:ipblack:info")
    public R info(@PathVariable("id") Long id) {
        IpBlackEntity ipBlack = ipBlackService.getById(id);
        return R.ok().put("ipBlack" , ipBlack);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:ipblack:save")
    public R save(@RequestBody IpBlackEntity ipBlack) {
        ipBlackService.save(ipBlack);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:ipblack:update")
    public R update(@RequestBody IpBlackEntity ipBlack) {
        ValidatorUtils.validateEntity(ipBlack);
        ipBlackService.updateById(ipBlack);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:ipblack:delete")
    public R delete(@RequestBody Long[] ids) {
        ipBlackService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
