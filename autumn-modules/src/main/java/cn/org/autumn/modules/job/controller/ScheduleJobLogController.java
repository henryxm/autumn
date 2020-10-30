package cn.org.autumn.modules.job.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.job.controller.gen.ScheduleJobLogControllerGen;



/**
 * 定时任务日志
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@RestController
@RequestMapping("job/schedulejoblog")
public class ScheduleJobLogController extends ScheduleJobLogControllerGen{

}
