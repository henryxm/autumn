package cn.org.autumn.modules.job.service;

import cn.org.autumn.annotation.TaskAware;
import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.dao.ScheduleJobDao;
import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.job.utils.ScheduleUtils;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.utils.SpringContextUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;

import static cn.org.autumn.modules.sys.service.SysMenuService.getSystemMenuKey;

@DependsOn("springContextUtils")
@Service
public class ScheduleJobService extends ModuleService<ScheduleJobDao, ScheduleJobEntity> implements InitFactory.Must, LoopJob.TenMinute {

    @Autowired
    private Scheduler scheduler;

    private void scanInit() {
        if (null != SpringContextUtils.applicationContext) {
            String[] beans = SpringContextUtils.applicationContext.getBeanDefinitionNames();
            for (String beanName : beans) {
                Class<?> beanType = SpringContextUtils.applicationContext.getType(beanName);
                Method[] methods = beanType.getMethods();
                for (Method method : methods) {
                    TaskAware taskAware = method.getAnnotation(TaskAware.class);
                    if (null != taskAware) {
                        ScheduleJobEntity scheduleJobEntity = new ScheduleJobEntity();
                        scheduleJobEntity.setBeanName(beanName);
                        scheduleJobEntity.setMethodName(method.getName());
                        ScheduleJobEntity entity = baseMapper.selectOne(scheduleJobEntity);
                        if (null == entity) {
                            scheduleJobEntity.setStatus(taskAware.status());
                            scheduleJobEntity.setCronExpression(taskAware.cronExpression());
                            scheduleJobEntity.setParams(taskAware.params());
                            scheduleJobEntity.setRemark(taskAware.remark());
                            scheduleJobEntity.setCreateTime(new Date());
                            scheduleJobEntity.setMode(taskAware.mode());
                            baseMapper.insert(scheduleJobEntity);
                        } else {
                            if (StringUtils.isEmpty(entity.getMode())) {
                                entity.setMode(taskAware.mode());
                                baseMapper.updateById(entity);
                            }
                        }
                    }
                }
            }
        }
    }

    public void init() {
        super.init();
        sysMenuService.put(menus());
    }

    public void must() {
        scanInit();
        initScheduler();
    }

    /**
     * 项目启动时，初始化定时器
     */
    public void initScheduler() {
        List<ScheduleJobEntity> scheduleJobList = this.selectList(null);
        for (ScheduleJobEntity scheduleJob : scheduleJobList) {
            CronTrigger cronTrigger = ScheduleUtils.getCronTrigger(scheduler, scheduleJob.getJobId());
            //如果不存在，则创建
            if (cronTrigger == null) {
                ScheduleUtils.createScheduleJob(scheduler, scheduleJob);
            } else {
                ScheduleUtils.updateScheduleJob(scheduler, scheduleJob);
            }
        }
    }

    public PageUtils queryPage(Map<String, Object> params) {
        Page<ScheduleJobEntity> _page = new Query<ScheduleJobEntity>(params).getPage();
        EntityWrapper<ScheduleJobEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();

        String beanName = (String) params.get("beanName");
        entityEntityWrapper.like(StringUtils.isNotBlank(beanName), "bean_name", beanName);

        _page.setCondition(condition);
        Page<ScheduleJobEntity> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(ScheduleJobEntity scheduleJob) {
        scheduleJob.setCreateTime(new Date());
        scheduleJob.setStatus(Constant.ScheduleStatus.NORMAL.getValue());
        this.insert(scheduleJob);
        ScheduleUtils.createScheduleJob(scheduler, scheduleJob);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ScheduleJobEntity scheduleJob) {
        ScheduleUtils.updateScheduleJob(scheduler, scheduleJob);
        this.updateById(scheduleJob);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] jobIds) {
        for (Long jobId : jobIds) {
            ScheduleUtils.deleteScheduleJob(scheduler, jobId);
        }
        this.deleteBatchIds(Arrays.asList(jobIds));
    }

    public int updateBatch(Long[] jobIds, int status) {
        return baseMapper.updateBatch(jobIds, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void run(Long[] jobIds) {
        for (Long jobId : jobIds) {
            ScheduleUtils.run(scheduler, this.selectById(jobId));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void pause(Long[] jobIds) {
        for (Long jobId : jobIds) {
            ScheduleUtils.pauseJob(scheduler, jobId);
        }
        updateBatch(jobIds, Constant.ScheduleStatus.PAUSE.getValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void resume(Long[] jobIds) {
        for (Long jobId : jobIds) {
            ScheduleUtils.resumeJob(scheduler, jobId);
        }
        updateBatch(jobIds, Constant.ScheduleStatus.NORMAL.getValue());
    }

    public String[][] menus() {
        String menuKey = SysMenuService.getMenuKey("Job", "ScheduleJob");
        return new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"日志列表", null, "job:schedulejob:log", "2", null, "0", getSystemMenuKey("JobScheduleLog"), menuKey, "sys_string_log_list"},
                {"立即执行", null, "job:schedulejob:run", "2", null, "0", getSystemMenuKey("JobScheduleRun"), menuKey, "sys_string_immediate_execution"},
                {"暂停", null, "job:schedulejob:pause", "2", null, "0", getSystemMenuKey("JobSchedulePause"), menuKey, "sys_string_suspend"},
                {"恢复", null, "job:schedulejob:resume", "2", null, "0", getSystemMenuKey("JobScheduleResume"), menuKey, "sys_string_resume"},
        };
    }

    @Override
    public void onTenMinute() {
        initScheduler();
    }
}
