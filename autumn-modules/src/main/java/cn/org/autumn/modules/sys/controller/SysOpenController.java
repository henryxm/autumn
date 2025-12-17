package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.entity.SysOpenEntity;
import cn.org.autumn.modules.sys.service.SysOpenService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 系统对接
 *
 * @author User
 * @email henryxm@163.com
 * @date 2025-12
 */
@RestController
@RequestMapping("sys/open")
public class SysOpenController {

    @Autowired
    protected SysOpenService sysOpenService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:open:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysOpenService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sys:open:info")
    public R info(@PathVariable("id") Long id) {
        SysOpenEntity open = sysOpenService.selectById(id);
        return R.ok().put("open", open);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sys:open:save")
    public R save(@RequestBody SysOpenEntity open) {
        sysOpenService.insert(open);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sys:open:update")
    public R update(@RequestBody SysOpenEntity open) {
        ValidatorUtils.validateEntity(open);
        sysOpenService.updateAllColumnById(open);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sys:open:delete")
    public R delete(@RequestBody Long[] ids) {
        sysOpenService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
