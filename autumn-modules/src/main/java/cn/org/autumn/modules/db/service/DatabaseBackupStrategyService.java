package cn.org.autumn.modules.db.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.db.dao.DatabaseBackupStrategyDao;
import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.utils.PageUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DatabaseBackupStrategyService extends ModuleService<DatabaseBackupStrategyDao, DatabaseBackupStrategyEntity> implements LoopJob.OneHour, LoopJob.OneDay {

    @Autowired
    private DatabaseBackupService databaseBackupService;

    /**
     * 分页查询策略
     */
    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(params, "id");
    }

    /**
     * 每小时检查：执行 HOURLY 策略
     */
    @Override
    public void onOneHour() {
        executeScheduledStrategies("HOURLY");
    }

    /**
     * 每天检查：执行 DAILY 和 WEEKLY 策略
     */
    @Override
    public void onOneDay() {
        executeScheduledStrategies("DAILY");
        executeScheduledStrategies("WEEKLY");
    }

    /**
     * 按调度类型执行策略
     */
    private void executeScheduledStrategies(String schedule) {
        try {
            List<DatabaseBackupStrategyEntity> strategies = selectList(new EntityWrapper<DatabaseBackupStrategyEntity>().eq("enable", true).eq("schedule", schedule));
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
        DatabaseBackupStrategyEntity strategy = selectById(strategyId);
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
