package cn.org.autumn.modules.spm.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import cn.org.autumn.modules.spm.service.VisitLogService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 访问统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class VisitLogControllerGen {

    @Autowired
    protected VisitLogService visitLogService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("spm:visitlog:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = visitLogService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("spm:visitlog:info")
    public R info(@PathVariable("id") Long id) {
        VisitLogEntity visitLog = visitLogService.getById(id);
        return R.ok().put("visitLog" , visitLog);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("spm:visitlog:save")
    public R save(@RequestBody VisitLogEntity visitLog) {
        visitLogService.save(visitLog);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("spm:visitlog:update")
    public R update(@RequestBody VisitLogEntity visitLog) {
        ValidatorUtils.validateEntity(visitLog);
        visitLogService.updateById(visitLog);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("spm:visitlog:delete")
    public R delete(@RequestBody Long[] ids) {
        visitLogService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
