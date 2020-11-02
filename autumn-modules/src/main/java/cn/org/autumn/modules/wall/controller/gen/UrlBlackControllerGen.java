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
import cn.org.autumn.modules.wall.entity.UrlBlackEntity;
import cn.org.autumn.modules.wall.service.UrlBlackService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 链接黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class UrlBlackControllerGen {

    @Autowired
    protected UrlBlackService urlBlackService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:urlblack:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = urlBlackService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:urlblack:info")
    public R info(@PathVariable("id") Long id) {
        UrlBlackEntity urlBlack = urlBlackService.selectById(id);
        return R.ok().put("urlBlack" , urlBlack);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:urlblack:save")
    public R save(@RequestBody UrlBlackEntity urlBlack) {
        urlBlackService.insert(urlBlack);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:urlblack:update")
    public R update(@RequestBody UrlBlackEntity urlBlack) {
        ValidatorUtils.validateEntity(urlBlack);
        urlBlackService.updateAllColumnById(urlBlack);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:urlblack:delete")
    public R delete(@RequestBody Long[] ids) {
        urlBlackService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
