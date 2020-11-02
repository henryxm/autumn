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
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * IP白名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class IpWhiteControllerGen {

    @Autowired
    protected IpWhiteService ipWhiteService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:ipwhite:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = ipWhiteService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:ipwhite:info")
    public R info(@PathVariable("id") Long id) {
        IpWhiteEntity ipWhite = ipWhiteService.selectById(id);
        return R.ok().put("ipWhite" , ipWhite);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:ipwhite:save")
    public R save(@RequestBody IpWhiteEntity ipWhite) {
        ipWhiteService.insert(ipWhite);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:ipwhite:update")
    public R update(@RequestBody IpWhiteEntity ipWhite) {
        ValidatorUtils.validateEntity(ipWhite);
        ipWhiteService.updateAllColumnById(ipWhite);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:ipwhite:delete")
    public R delete(@RequestBody Long[] ids) {
        ipWhiteService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
