package cn.org.autumn.modules.job.service;

import cn.org.autumn.modules.job.service.gen.ScheduleJobLogServiceGen;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.stereotype.Service;

@Service
public class ScheduleJobLogService extends ScheduleJobLogServiceGen implements LoopJob.Job {

    @Override
    public int menuOrder() {
        return 5;
    }

    @Override
    public String ico() {
        return "fa-bars";
    }

    public void init() {
        super.init();
        LoopJob.onOneHour(this);
    }

    public String parentMenu() {
        super.parentMenu();
        return SysMenuService.getSystemManagementMenuKey();
    }

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {"job_schedulejoblog_table_comment", "任务日志", "Task log"},
                {"job_schedulejoblog_column_log_id", "任务日志id", "Log ID"},
                {"job_schedulejoblog_column_job_id", "任务id", "Task ID"},
                {"job_schedulejoblog_column_bean_name", "BeanName", "Bean name"},
                {"job_schedulejoblog_column_method_name", "方法名", "Method name"},
                {"job_schedulejoblog_column_params", "参数", "Parameter"},
                {"job_schedulejoblog_column_status", "任务状态", "Task status"},
                {"job_schedulejoblog_column_error", "失败信息", "Fail message"},
                {"job_schedulejoblog_column_times", "耗时(单位：毫秒)", "Duration(unit:millisecond)"},
                {"job_schedulejoblog_column_create_time", "创建时间", "Create time"},
        };
        return items;
    }

    @Override
    public void runJob() {
        baseMapper.clear();
    }
}
