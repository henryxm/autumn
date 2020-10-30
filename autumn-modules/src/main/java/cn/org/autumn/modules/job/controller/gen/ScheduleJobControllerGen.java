package cn.org.autumn.modules.job.controller.gen;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.modules.job.service.ScheduleJobService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 定时任务
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
public class ScheduleJobControllerGen {

    @Autowired
    protected ScheduleJobService scheduleJobService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("job:schedulejob:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = scheduleJobService.queryPage(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{jobId}")
    @RequiresPermissions("job:schedulejob:info")
    public R info(@PathVariable("jobId") Long jobId){
        ScheduleJobEntity scheduleJob = scheduleJobService.selectById(jobId);
        return R.ok().put("scheduleJob", scheduleJob);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("job:schedulejob:save")
    public R save(@RequestBody ScheduleJobEntity scheduleJob){
        scheduleJobService.insert(scheduleJob);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("job:schedulejob:update")
    public R update(@RequestBody ScheduleJobEntity scheduleJob){
        scheduleJobService.updateAllColumnById(scheduleJob);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("job:schedulejob:delete")
    public R delete(@RequestBody Long[] jobIds){
        scheduleJobService.deleteBatchIds(Arrays.asList(jobIds));
        return R.ok();
    }

}
