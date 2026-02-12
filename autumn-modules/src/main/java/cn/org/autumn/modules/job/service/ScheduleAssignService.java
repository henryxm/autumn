package cn.org.autumn.modules.job.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.dao.ScheduleAssignDao;
import cn.org.autumn.modules.job.entity.ScheduleAssignEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.thread.TagValue;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 定时任务服务器分配服务
 * <p>
 * 负责在启动后扫描所有定时任务，将任务信息及服务器分配配置同步到数据库表中，
 * 并在运行时支持通过管理界面动态修改分配配置。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>启动延迟扫描：等待所有任务注册完成后，扫描并同步到 sys_schedule_assign 表</li>
 *   <li>数据库 → 内存同步：从数据库加载分配配置到 LoopJob 的内存中</li>
 *   <li>管理界面更新：支持通过 API 更新分配标签，同时持久化到数据库和内存</li>
 *   <li>定时刷新：每分钟从数据库刷新分配配置，支持多实例部署场景</li>
 * </ul>
 */
@Slf4j
@Service
public class ScheduleAssignService extends ModuleService<ScheduleAssignDao, ScheduleAssignEntity> implements LoadFactory.Must, LoopJob.TenMinute {

    @Autowired
    TagTaskExecutor asyncTaskExecutor;

    @Override
    public void must() {
        scanAndSync();
    }

    @Override
    public void onTenMinute() {
        asyncTaskExecutor.execute(new TagRunnable() {
            @Override
            @TagValue(type = ScheduleAssignService.class, method = "onTenMinute", tag = "刷新定时任务分配")
            public void exe() {
                safeRefreshAssignments();
            }
        });
    }

    /**
     * 安全执行刷新（捕获异常）
     */
    private void safeRefreshAssignments() {
        try {
            if (LoopJob.isAssignInitialized()) {
                refreshAssignments();
            }
        } catch (Exception e) {
            log.error("Schedule assign refresh failed: {}", e.getMessage(), e);
        }
    }

    public void scanAndSync() {
        asyncTaskExecutor.execute(new TagRunnable() {
            @Override
            public boolean can() {
                return true;
            }

            @Override
            @TagValue(type = ScheduleAssignService.class, method = "scanAndSync", tag = "扫码定时任务情况", delay = 5)
            public void exe() {
                scanAndSyncInternal();
            }
        });
    }

    /**
     * 扫描所有已注册的定时任务，并与数据库同步
     * <p>
     * 1. 获取所有已注册的 JobInfo
     * 2. 获取数据库中现有的分配记录
     * 3. 新任务 → 创建数据库记录（使用注解默认值）
     * 4. 已有任务 → 更新基本信息，保留用户修改的 assignTag
     * 5. 从数据库加载 assignTag 到内存
     */
    private void scanAndSyncInternal() {
        Map<String, LoopJob.JobInfo> jobInfoMap = LoopJob.getJobInfoMap();
        if (jobInfoMap.isEmpty()) {
            log.warn("No jobs registered, skip scan");
            return;
        }
        if (log.isDebugEnabled())
            log.debug("Start scanning {} registered jobs...", jobInfoMap.size());
        // 加载数据库中已有的分配记录，以 jobId 为 key
        Map<String, ScheduleAssignEntity> existingMap = new HashMap<>();
        try {
            List<ScheduleAssignEntity> existingList = this.selectList(null);
            if (existingList != null) {
                for (ScheduleAssignEntity entity : existingList) {
                    if (entity.getJobId() != null) {
                        existingMap.put(entity.getJobId(), entity);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load existing assignments from DB: {}", e.getMessage(), e);
        }
        int created = 0, updated = 0;
        for (Map.Entry<String, LoopJob.JobInfo> entry : jobInfoMap.entrySet()) {
            String jobId = entry.getKey();
            LoopJob.JobInfo info = entry.getValue();
            ScheduleAssignEntity existing = existingMap.get(jobId);
            if (existing == null) {
                // 新任务：创建数据库记录
                try {
                    ScheduleAssignEntity entity = new ScheduleAssignEntity();
                    entity.setJobId(jobId);
                    entity.setJobName(info.getDisplayName());
                    entity.setClassName(info.getClassName());
                    entity.setCategory(info.getCategory());
                    entity.setCategoryDisplayName(info.getCategoryDisplayName());
                    entity.setAssignTag(info.getDefaultAssignTag());
                    entity.setDefaultAssignTag(info.getDefaultAssignTag());
                    entity.setGroupName(info.getGroup());
                    entity.setDescription(info.getDescription());
                    entity.setEnabled(info.isEnabled() ? 1 : 0);
                    entity.setUpdateTime(new Date());
                    this.insert(entity);
                    created++;
                } catch (Exception e) {
                    log.error("Failed to create assignment for job [{}]: {}", jobId, e.getMessage());
                }
            } else {
                // 已有任务：更新基本信息，保留用户修改的 assignTag
                try {
                    boolean needUpdate = false;
                    if (!Objects.equals(existing.getJobName(), info.getDisplayName())) {
                        existing.setJobName(info.getDisplayName());
                        needUpdate = true;
                    }
                    if (!Objects.equals(existing.getClassName(), info.getClassName())) {
                        existing.setClassName(info.getClassName());
                        needUpdate = true;
                    }
                    if (!Objects.equals(existing.getCategory(), info.getCategory())) {
                        existing.setCategory(info.getCategory());
                        needUpdate = true;
                    }
                    if (!Objects.equals(existing.getCategoryDisplayName(), info.getCategoryDisplayName())) {
                        existing.setCategoryDisplayName(info.getCategoryDisplayName());
                        needUpdate = true;
                    }
                    if (!Objects.equals(existing.getDefaultAssignTag(), info.getDefaultAssignTag())) {
                        existing.setDefaultAssignTag(info.getDefaultAssignTag());
                        needUpdate = true;
                    }
                    if (!Objects.equals(existing.getGroupName(), info.getGroup())) {
                        existing.setGroupName(info.getGroup());
                        needUpdate = true;
                    }
                    if (!Objects.equals(existing.getDescription(), info.getDescription())) {
                        existing.setDescription(info.getDescription());
                        needUpdate = true;
                    }
                    if (needUpdate) {
                        existing.setUpdateTime(new Date());
                        this.updateById(existing);
                        updated++;
                    }
                } catch (Exception e) {
                    log.error("Failed to update assignment for job [{}]: {}", jobId, e.getMessage());
                }
                // 从数据库加载 assignTag 到内存（数据库配置优先于注解）
                String dbAssignTag = existing.getAssignTag();
                if (dbAssignTag != null) {
                    LoopJob.updateJobAssign(jobId, dbAssignTag);
                }
            }
        }
        // 标记分配功能已初始化
        LoopJob.markAssignInitialized();
        if (log.isDebugEnabled())
            log.debug("Schedule assign scan completed: {} created, {} updated, total {} jobs", created, updated, jobInfoMap.size());
    }

    /**
     * 从数据库刷新所有分配配置到内存
     * <p>
     * 用于定时刷新和多实例同步场景
     */
    public void refreshAssignments() {
        try {
            List<ScheduleAssignEntity> list = this.selectList(null);
            if (list == null || list.isEmpty()) return;
            int refreshed = 0;
            for (ScheduleAssignEntity entity : list) {
                if (entity.getJobId() != null) {
                    boolean result = LoopJob.updateJobAssign(entity.getJobId(), entity.getAssignTag() != null ? entity.getAssignTag() : "");
                    if (result)
                        refreshed++;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Refreshed {} assign configs from database", refreshed);
            }
        } catch (Exception e) {
            log.error("Failed to refresh assignments: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新指定任务的服务器分配标签
     * <p>
     * 同时更新数据库和内存配置
     *
     * @param jobId     任务ID
     * @param assignTag 分配标签（逗号分隔），为空表示在所有服务器运行
     * @return true: 更新成功; false: 失败
     */
    public boolean updateAssignment(String jobId, String assignTag) {
        if (StringUtils.isBlank(jobId)) return false;
        String tag = assignTag != null ? assignTag.trim() : "";
        // 更新内存
        boolean memoryUpdated = LoopJob.updateJobAssign(jobId, tag);
        if (!memoryUpdated) {
            log.warn("Job [{}] not found in memory", jobId);
        }
        // 更新数据库
        try {
            ScheduleAssignEntity entity = this.selectOne(new EntityWrapper<ScheduleAssignEntity>().eq("job_id", jobId));
            if (entity != null) {
                entity.setAssignTag(tag);
                entity.setUpdateTime(new Date());
                this.updateById(entity);
                if (log.isDebugEnabled())
                    log.debug("Assignment updated for job [{}]: '{}'", jobId, tag);
                return true;
            } else {
                log.warn("Job [{}] not found in database, creating...", jobId);
                // 尝试从 LoopJob 获取信息并创建
                Map<String, LoopJob.JobInfo> jobInfoMap = LoopJob.getJobInfoMap();
                LoopJob.JobInfo info = jobInfoMap.get(jobId);
                if (info != null) {
                    ScheduleAssignEntity newEntity = new ScheduleAssignEntity();
                    newEntity.setJobId(jobId);
                    newEntity.setJobName(info.getDisplayName());
                    newEntity.setClassName(info.getClassName());
                    newEntity.setCategory(info.getCategory());
                    newEntity.setCategoryDisplayName(info.getCategoryDisplayName());
                    newEntity.setAssignTag(tag);
                    newEntity.setDefaultAssignTag(info.getDefaultAssignTag());
                    newEntity.setGroupName(info.getGroup());
                    newEntity.setDescription(info.getDescription());
                    newEntity.setEnabled(info.isEnabled() ? 1 : 0);
                    newEntity.setUpdateTime(new Date());
                    this.insert(newEntity);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to update assignment for job [{}]: {}", jobId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取所有分配记录
     */
    public List<ScheduleAssignEntity> getAllAssignments() {
        try {
            List<ScheduleAssignEntity> list = this.selectList(null);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get all assignments: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据任务ID获取分配记录
     */
    public ScheduleAssignEntity getByJobId(String jobId) {
        if (StringUtils.isBlank(jobId)) return null;
        try {
            return this.selectOne(new EntityWrapper<ScheduleAssignEntity>().eq("job_id", jobId));
        } catch (Exception e) {
            log.error("Failed to get assignment for job [{}]: {}", jobId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 手动触发重新扫描同步
     */
    public void triggerRescan() {
        scanAndSync();
    }
}
