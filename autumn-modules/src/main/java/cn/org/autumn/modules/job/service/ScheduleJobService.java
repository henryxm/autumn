/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.job.service;

import cn.org.autumn.annotation.TaskAware;
import cn.org.autumn.table.TableInit;
import cn.org.autumn.utils.SpringContextUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.job.dao.ScheduleJobDao;
import cn.org.autumn.modules.job.entity.ScheduleJobEntity;
import cn.org.autumn.modules.job.utils.ScheduleUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;

@Service("scheduleJobService")
@DependsOn("springContextUtils")
public class ScheduleJobService extends ServiceImpl<ScheduleJobDao, ScheduleJobEntity> {
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ScheduleJobDao scheduleJobDao;

    @Autowired
    private TableInit tableInit;

    private int wEveryCount = 0;

    private static final String NULL = null;

    private void scanInit() {
        List<TaskAware> list = new ArrayList<>();
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
                        ScheduleJobEntity entity = scheduleJobDao.selectOne(scheduleJobEntity);
                        if (null == entity) {
                            scheduleJobEntity.setStatus(taskAware.status());
                            scheduleJobEntity.setCronExpression(taskAware.cronExpression());
                            scheduleJobEntity.setParams(taskAware.params());
                            scheduleJobEntity.setRemark(taskAware.remark());
                            scheduleJobEntity.setCreateTime(new Date());
                            scheduleJobEntity.setMode(taskAware.mode());
                            scheduleJobDao.insert(scheduleJobEntity);
                        } else {
                            if (StringUtils.isEmpty(entity.getMode())) {
                                entity.setMode(taskAware.mode());
                                scheduleJobDao.updateById(entity);
                            }
                        }
                        list.add(taskAware);
                    }
                }

            }
        }

    }

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        scanInit();
        String[][] mapping = new String[][]{};
        for (String[] map : mapping) {
            ScheduleJobEntity sysMenu = new ScheduleJobEntity();
            String temp = map[0];
            if (NULL != temp)
                sysMenu.setBeanName(temp);
            temp = map[1];
            if (NULL != temp)
                sysMenu.setMethodName(temp);
            ScheduleJobEntity entity = scheduleJobDao.selectOne(sysMenu);

            if (null == entity) {
                temp = map[2];
                if (NULL != temp)
                    sysMenu.setParams(temp);
                temp = map[3];
                if (NULL != temp)
                    sysMenu.setCronExpression(temp);
                temp = map[4];
                if (NULL != temp)
                    sysMenu.setStatus(Integer.valueOf(temp));
                temp = map[5];
                if (NULL != temp)
                    sysMenu.setRemark(temp);
                sysMenu.setCreateTime(new Date());
                scheduleJobDao.insert(sysMenu);
            }
        }
        initScheduler();
    }

    /**
     * 项目启动时，初始化定时器
     */
    @PostConstruct
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

    public void load(int i) {
        if (wEveryCount < i) {
            wEveryCount++;
            return;
        }
        wEveryCount = 0;
        initScheduler();
    }

    public PageUtils queryPage(Map<String, Object> params) {
        Page<ScheduleJobEntity> _page = new Query<ScheduleJobEntity>(params).getPage();
        EntityWrapper<ScheduleJobEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String, Object> condition = new HashMap<>();

        String beanName = (String) params.get("beanName");
        entityEntityWrapper.like(StringUtils.isNotBlank(beanName), "bean_name" , beanName);

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
}
