package cn.org.autumn.modules.job.controller;

import cn.org.autumn.annotation.SysLog;
import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import cn.org.autumn.modules.job.controller.gen.ScheduleJobControllerGen;

/**
 * 定时任务
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@RestController
@RequestMapping("job/schedulejob")
public class ScheduleJobController extends ScheduleJobControllerGen {
    /**
     * 保存定时任务
     */
    @SysLog("保存定时任务")
    @RequestMapping("/save")
    @RequiresPermissions("job:schedulejob:save")
    public R save(@RequestBody ScheduleJobEntity scheduleJob) {
        ValidatorUtils.validateEntity(scheduleJob);
        scheduleJobService.save(scheduleJob);
        return R.ok();
    }

    /**
     * 修改定时任务
     */
    @SysLog("修改定时任务")
    @RequestMapping("/update")
    @RequiresPermissions("job:schedulejob:update")
    public R update(@RequestBody ScheduleJobEntity scheduleJob) {
        ValidatorUtils.validateEntity(scheduleJob);
        scheduleJobService.update(scheduleJob);
        return R.ok();
    }

    /**
     * 删除定时任务
     */
    @SysLog("删除定时任务")
    @RequestMapping("/delete")
    @RequiresPermissions("job:schedulejob:delete")
    public R delete(@RequestBody Long[] jobIds) {
        scheduleJobService.deleteBatch(jobIds);
        return R.ok();
    }

    /**
     * 立即执行任务
     */
    @SysLog("立即执行任务")
    @RequestMapping("/run")
    @RequiresPermissions("job:schedulejob:run")
    public R run(@RequestBody Long[] jobIds) {
        scheduleJobService.run(jobIds);
        return R.ok();
    }

    /**
     * 暂停定时任务
     */
    @SysLog("暂停定时任务")
    @RequestMapping("/pause")
    @RequiresPermissions("job:schedulejob:pause")
    public R pause(@RequestBody Long[] jobIds) {
        scheduleJobService.pause(jobIds);
        return R.ok();
    }

    /**
     * 恢复定时任务
     */
    @SysLog("恢复定时任务")
    @RequestMapping("/resume")
    @RequiresPermissions("job:schedulejob:resume")
    public R resume(@RequestBody Long[] jobIds) {
        scheduleJobService.resume(jobIds);
        return R.ok();
    }
}
