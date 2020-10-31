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
import cn.org.autumn.modules.job.dao.ScheduleJobDao;
import cn.org.autumn.modules.job.entity.ScheduleJobEntity;

import javax.annotation.PostConstruct;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 定时任务控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

public class ScheduleJobServiceGen extends ServiceImpl<ScheduleJobDao, ScheduleJobEntity> {

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
        Page<ScheduleJobEntity> _page = new Query<ScheduleJobEntity>(params).getPage();
        EntityWrapper<ScheduleJobEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();
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
        if (params.containsKey("cronExpression") && null != params.get("cronExpression") && StringUtils.isNotEmpty(params.get("cronExpression").toString())) {
            condition.put("cron_expression", params.get("cronExpression"));
        }
        if (params.containsKey("status") && null != params.get("status") && StringUtils.isNotEmpty(params.get("status").toString())) {
            condition.put("status", params.get("status"));
        }
        if (params.containsKey("mode") && null != params.get("mode") && StringUtils.isNotEmpty(params.get("mode").toString())) {
            condition.put("mode", params.get("mode"));
        }
        if (params.containsKey("remark") && null != params.get("remark") && StringUtils.isNotEmpty(params.get("remark").toString())) {
            condition.put("remark", params.get("remark"));
        }
        if (params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<ScheduleJobEntity> page = this.selectPage(_page, entityEntityWrapper);
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
        return 1;
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
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("job_schedulejob_table_comment", "定时任务", "Schedule job");
        languageService.addLanguageColumnItem("job_schedulejob_column_job_id", "任务id", "Task ID");
        languageService.addLanguageColumnItem("job_schedulejob_column_bean_name", "Bean名称", "Bean name");
        languageService.addLanguageColumnItem("job_schedulejob_column_method_name", "方法名", "Method name");
        languageService.addLanguageColumnItem("job_schedulejob_column_params", "参数", "Parameter");
        languageService.addLanguageColumnItem("job_schedulejob_column_cron_expression", "Cron表达式", "Cron express");
        languageService.addLanguageColumnItem("job_schedulejob_column_status", "任务状态", "Task status");
        languageService.addLanguageColumnItem("job_schedulejob_column_mode", "任务执行模式", "Task run mode");
        languageService.addLanguageColumnItem("job_schedulejob_column_remark", "备注", "Remark");
        languageService.addLanguageColumnItem("job_schedulejob_column_create_time", "创建时间", "Create time");
    }
}
