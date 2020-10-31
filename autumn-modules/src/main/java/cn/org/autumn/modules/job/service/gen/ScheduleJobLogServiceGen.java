package cn.org.autumn.modules.job.service.gen;

import cn.org.autumn.table.TableInit;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;

import cn.org.autumn.modules.job.service.JobMenu;
import cn.org.autumn.modules.job.dao.ScheduleJobLogDao;
import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;

import javax.annotation.PostConstruct;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 定时任务日志控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

public class ScheduleJobLogServiceGen extends ServiceImpl<ScheduleJobLogDao, ScheduleJobLogEntity> {

    protected static final String NULL = null;

    @Autowired
    protected JobMenu jobMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<ScheduleJobLogEntity> _page = new Query<ScheduleJobLogEntity>(params).getPage();
        EntityWrapper<ScheduleJobLogEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();
        if (params.containsKey("logId") && null != params.get("logId") && StringUtils.isNotEmpty(params.get("logId").toString())) {
            condition.put("log_id", params.get("logId"));
        }
        if (params.containsKey("jobId") && null != params.get("jobId") && StringUtils.isNotEmpty(params.get("jobId").toString())) {
            condition.put("job_id", params.get("jobId"));
        }
        if (params.containsKey("beanName") && null != params.get("beanName") && StringUtils.isNotEmpty(params.get("beanName").toString())) {
            condition.put("bean_name", params.get("beanName"));
        }
        if (params.containsKey("methodName") && null != params.get("methodName") && StringUtils.isNotEmpty(params.get("methodName").toString())) {
            condition.put("method_name", params.get("methodName"));
        }
        if (params.containsKey("params") && null != params.get("params") && StringUtils.isNotEmpty(params.get("params").toString())) {
            condition.put("params", params.get("params"));
        }
        if (params.containsKey("status") && null != params.get("status") && StringUtils.isNotEmpty(params.get("status").toString())) {
            condition.put("status", params.get("status"));
        }
        if (params.containsKey("error") && null != params.get("error") && StringUtils.isNotEmpty(params.get("error").toString())) {
            condition.put("error", params.get("error"));
        }
        if (params.containsKey("times") && null != params.get("times") && StringUtils.isNotEmpty(params.get("times").toString())) {
            condition.put("times", params.get("times"));
        }
        if (params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<ScheduleJobLogEntity> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    /**
     * need implement it in the subclass.
     *
     * @return
     */
    public int menuOrder() {
        return 0;
    }

    /**
     * need implement it in the subclass.
     *
     * @return
     */
    public int parentMenu() {
        jobMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(JobMenu.job_menu);
        if (null != sysMenuEntity)
            return sysMenuEntity.getMenuId().intValue();
        return 6;
    }

    public String ico() {
        return "fa-file-code-o";
    }

    private String order() {
        return String.valueOf(menuOrder());
    }

    private String parent() {
        return String.valueOf(parentMenu());
    }


    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, parent(), "任务日志", "modules/job/schedulejoblog", "job:schedulejoblog:list,job:schedulejoblog:info,job:schedulejoblog:save,job:schedulejoblog:update,job:schedulejoblog:delete", "1", "fa " + ico(), order(), "", "job_schedulejoblog_table_comment"};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "job:schedulejoblog:list,job:schedulejoblog:info", "2", null, order(), "", "sys_string_lookup"},
                {null, id + "", "新增", null, "job:schedulejoblog:save", "2", null, order(), "", "sys_string_add"},
                {null, id + "", "修改", null, "job:schedulejoblog:update", "2", null, order(), "", "sys_string_change"},
                {null, id + "", "删除", null, "job:schedulejoblog:delete", "2", null, order(), "", "sys_string_delete"},
        };
        for (String[] menu : menus) {
            sysMenu = sysMenuService.from(menu);
            entity = sysMenuService.get(sysMenu);
            if (null == entity) {
                sysMenuService.put(sysMenu);
            }
        }
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("job_schedulejoblog_table_comment", "任务日志", "Task log");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_log_id", "日志id", "Log ID");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_job_id", "任务id", "Task ID");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_bean_name", "Bean名称", "Bean name");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_method_name", "方法名", "Method name");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_params", "参数", "Parameter");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_status", "任务状态", "Task status");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_error", "失败信息", "Fail message");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_times", "耗时(单位：毫秒)", "Duration(unit:millisecond)");
        languageService.addLanguageColumnItem("job_schedulejoblog_column_create_time", "创建时间", "Create time");
    }
}
