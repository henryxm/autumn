package cn.org.autumn.modules.job.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.job.entity.ScheduleAssignEntity;
import cn.org.autumn.modules.job.service.ScheduleAssignService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 定时分配
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
public class ScheduleAssignControllerGen {

    @Autowired
    protected ScheduleAssignService scheduleAssignService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("job:scheduleassign:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = scheduleAssignService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("job:scheduleassign:info")
    public R info(@PathVariable("id") Long id) {
        ScheduleAssignEntity scheduleAssign = scheduleAssignService.getById(id);
        return R.ok().put("scheduleAssign" , scheduleAssign);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("job:scheduleassign:save")
    public R save(@RequestBody ScheduleAssignEntity scheduleAssign) {
        scheduleAssignService.save(scheduleAssign);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("job:scheduleassign:update")
    public R update(@RequestBody ScheduleAssignEntity scheduleAssign) {
        ValidatorUtils.validateEntity(scheduleAssign);
        scheduleAssignService.updateById(scheduleAssign);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("job:scheduleassign:delete")
    public R delete(@RequestBody Long[] ids) {
        scheduleAssignService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
