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
import cn.org.autumn.modules.wall.entity.IpVisitEntity;
import cn.org.autumn.modules.wall.service.IpVisitService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * IP访问表
 *
 * @author User
 * @email henryxm@163.com
 * @date 2021-11
 */
public class IpVisitControllerGen {

    @Autowired
    protected IpVisitService ipVisitService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("wall:ipvisit:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = ipVisitService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("wall:ipvisit:info")
    public R info(@PathVariable("id") Long id) {
        IpVisitEntity ipVisit = ipVisitService.selectById(id);
        return R.ok().put("ipVisit" , ipVisit);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("wall:ipvisit:save")
    public R save(@RequestBody IpVisitEntity ipVisit) {
        ipVisitService.insert(ipVisit);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("wall:ipvisit:update")
    public R update(@RequestBody IpVisitEntity ipVisit) {
        ValidatorUtils.validateEntity(ipVisit);
        ipVisitService.updateAllColumnById(ipVisit);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("wall:ipvisit:delete")
    public R delete(@RequestBody Long[] ids) {
        ipVisitService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
