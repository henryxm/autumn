package cn.org.autumn.modules.usr.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.usr.entity.UserOpenEntity;
import cn.org.autumn.modules.usr.service.UserOpenService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 认证对接
 *
 * @author User
 * @email henryxm@163.com
 * @date 2025-12
 */
public class UserOpenControllerGen {

    @Autowired
    protected UserOpenService userOpenService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("usr:useropen:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = userOpenService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("usr:useropen:info")
    public R info(@PathVariable("id") Long id) {
        UserOpenEntity userOpen = userOpenService.selectById(id);
        return R.ok().put("userOpen" , userOpen);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("usr:useropen:save")
    public R save(@RequestBody UserOpenEntity userOpen) {
        userOpenService.insert(userOpen);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("usr:useropen:update")
    public R update(@RequestBody UserOpenEntity userOpen) {
        ValidatorUtils.validateEntity(userOpen);
        userOpenService.updateAllColumnById(userOpen);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("usr:useropen:delete")
    public R delete(@RequestBody Long[] ids) {
        userOpenService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
