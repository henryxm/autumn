package cn.org.autumn.modules.job.controller;

import cn.org.autumn.modules.job.utils.ScheduleJob;
import cn.org.autumn.utils.R;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.job.controller.gen.ScheduleJobLogControllerGen;

@RestController
@RequestMapping("job/schedulejoblog")
public class ScheduleJobLogController extends ScheduleJobLogControllerGen {

    @ResponseBody
    @RequestMapping(value = {"/toggle"})
    public R joblog() {
        ScheduleJob.toggle();
        return R.ok();
    }
}
