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
import cn.org.autumn.modules.wall.entity.HostEntity;
import cn.org.autumn.modules.wall.service.HostService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 主机统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class HostControllerGen {

    @Autowired
    protected HostService hostService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:host:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = hostService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:host:info")
    public R info(@PathVariable("id") Long id) {
        HostEntity host = hostService.getById(id);
        return R.ok().put("host" , host);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:host:save")
    public R save(@RequestBody HostEntity host) {
        hostService.save(host);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:host:update")
    public R update(@RequestBody HostEntity host) {
        ValidatorUtils.validateEntity(host);
        hostService.updateById(host);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:host:delete")
    public R delete(@RequestBody Long[] ids) {
        hostService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
