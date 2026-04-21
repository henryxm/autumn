package cn.org.autumn.modules.db.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.db.dao.DatabaseBackupStrategyDao;
import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.thread.TagValue;
import cn.org.autumn.utils.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 定时备份策略调度。
 * <p>集群多实例部署时，{@link LoopJob} 会在每个节点触发；通过 {@code @TagValue(lock=true)} 配合 Redisson，
 * 保证同一调度周期内仅<strong>一个节点</strong>执行策略扫描与 {@link DatabaseBackupService#backupByStrategy}，
 * 避免重复备份与数据库压力倍增。需启用 Redis/Redisson；无 Redisson 时框架会单机回退（各节点仍可能各执行一次，见 {@code docs/AI_DISTRIBUTED_LOCK.md}）。</p>
 * <p>不参与备份：优先在本实例设置 {@code autumn.backup.exclude=true}；亦可选用 {@code autumn.backup.exclude-hosts}
 * / {@code exclude-addresses} 集中排除。排除节点不参加定时触发，手动执行策略与发起备份也会被拒绝。</p>
 */
@Slf4j
@Service
public class DatabaseBackupStrategyService extends ModuleService<DatabaseBackupStrategyDao, DatabaseBackupStrategyEntity> implements LoopJob.OneHour, LoopJob.OneDay {

    /** 与每小时调度对齐：锁租约内整集群最多触发一轮 HOURLY 策略 */
    private static final long CLUSTER_LOCK_LEASE_ONE_HOUR_SEC = 3600L;
    /** 与日调度对齐：锁租约内整集群最多触发一轮 DAILY/WEEKLY 策略检查 */
    private static final long CLUSTER_LOCK_LEASE_ONE_DAY_SEC = 86400L;

    @Autowired
    private DatabaseBackupService databaseBackupService;

    @Autowired
    private DatabaseBackupExecutionGuard databaseBackupExecutionGuard;

    @Autowired
    TagTaskExecutor asyncTaskExecutor;

    /**
     * 分页查询策略
     */
    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(params, columnInWrapper("id"));
    }

    /**
     * 每小时检查：执行 HOURLY 策略
     */
    @Override
    public void onOneHour() {
        if (databaseBackupExecutionGuard.isExcluded()) {
            if (log.isDebugEnabled()) {
                log.debug("跳过每小时备份策略调度（当前节点已 autumn.backup.exclude=true 或命中 exclude-hosts/exclude-addresses）");
            }
            return;
        }
        asyncTaskExecutor.execute(new TagRunnable() {
            @Override
            @TagValue(
                    lock = true,
                    time = CLUSTER_LOCK_LEASE_ONE_HOUR_SEC,
                    delay = 30,
                    method = "onOneHour",
                    type = DatabaseBackupStrategyService.class,
                    tag = "每小时备份策略")
            public void exe() {
                executeScheduledStrategies("HOURLY");
            }
        });
    }

    /**
     * 每天检查：执行 DAILY 和 WEEKLY 策略
     */
    @Override
    public void onOneDay() {
        if (databaseBackupExecutionGuard.isExcluded()) {
            if (log.isDebugEnabled()) {
                log.debug("跳过每日备份策略调度（当前节点已 autumn.backup.exclude=true 或命中 exclude-hosts/exclude-addresses）");
            }
            return;
        }
        asyncTaskExecutor.execute(new TagRunnable() {
            @Override
            @TagValue(
                    lock = true,
                    time = CLUSTER_LOCK_LEASE_ONE_DAY_SEC,
                    delay = 60,
                    method = "onOneDay",
                    type = DatabaseBackupStrategyService.class,
                    tag = "每天备份策略")
            public void exe() {
                executeScheduledStrategies("DAILY");
                executeScheduledStrategies("WEEKLY");
            }
        });
    }

    /**
     * 按调度类型执行策略
     */
    private void executeScheduledStrategies(String schedule) {
        try {
            List<DatabaseBackupStrategyEntity> strategies = list(new QueryWrapper<DatabaseBackupStrategyEntity>()
                    .eq(columnInWrapper("enable"), true).eq(columnInWrapper("schedule"), schedule));
            for (DatabaseBackupStrategyEntity strategy : strategies) {
                // WEEKLY 策略：检查距离上次执行是否超过7天
                if ("WEEKLY".equals(schedule) && strategy.getLastRunTime() != null) {
                    long daysSinceLastRun = (System.currentTimeMillis() - strategy.getLastRunTime().getTime()) / (1000 * 60 * 60 * 24);
                    if (daysSinceLastRun < 7) {
                        continue;
                    }
                }
                try {
                    if (log.isDebugEnabled())
                        log.debug("Executing scheduled backup strategy: id={}, name={}, schedule={}", strategy.getId(), strategy.getName(), schedule);
                    databaseBackupService.backupByStrategy(strategy);
                    // 更新上次执行时间
                    strategy.setLastRunTime(new Date());
                    updateById(strategy);
                    // 滚动备份清理：启用滚动且设置了保留数量时执行
                    if (Boolean.TRUE.equals(strategy.getRollingEnabled()) && strategy.getMaxKeep() != null && strategy.getMaxKeep() > 0) {
                        databaseBackupService.cleanupByStrategy(strategy.getId(), strategy.getMaxKeep());
                    }
                } catch (Exception e) {
                    log.error("Failed to execute backup strategy: id={}, name={}, error={}", strategy.getId(), strategy.getName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load backup strategies for schedule={}: {}", schedule, e.getMessage(), e);
        }
    }

    /**
     * 手动执行策略
     */
    public void executeStrategy(Long strategyId) {
        databaseBackupExecutionGuard.assertBackupAllowed();
        DatabaseBackupStrategyEntity strategy = getById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在");
        }
        if (!Boolean.TRUE.equals(strategy.getEnable())) {
            throw new RuntimeException("策略未启用");
        }
        databaseBackupService.backupByStrategy(strategy);
        strategy.setLastRunTime(new Date());
        updateById(strategy);
        // 滚动备份清理
        if (Boolean.TRUE.equals(strategy.getRollingEnabled()) && strategy.getMaxKeep() != null && strategy.getMaxKeep() > 0) {
            databaseBackupService.cleanupByStrategy(strategy.getId(), strategy.getMaxKeep());
        }
    }
}
