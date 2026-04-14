package cn.org.autumn.examples.distributedlock;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.service.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 示例：周期任务防重入三种模式。
 */
@Slf4j
@Component
public class LoopJobReentryGuardExample implements LoopJob.OneMinute {

    @Autowired
    private DistributedLockService distributedLockService;

    @Override
    public void onOneMinute() {
        // 默认采用 fallback：未抢到锁时跳过本轮
        runFallbackMode();
    }

    public void runStrictMode() {
        final String lockKey = "job:billing:close";
        distributedLockService.withLockUnchecked(lockKey, this::runJob);
    }

    public void runFallbackMode() {
        final String lockKey = "job:billing:close";
        distributedLockService.withLockOrFallbackUnchecked(lockKey, () -> {
            runJob();
            return null;
        }, (key, ex) -> {
            // 非持锁实例直接跳过，避免重复执行
            log.debug("job skipped key={} err={}", key, ex.toString());
            return null;
        });
    }

    public void runRetryMode() {
        final String lockKey = "job:billing:close";
        distributedLockService.withLockRetryUnchecked(lockKey, () -> {
            runJob();
            return null;
        });
    }

    private void runJob() {
        // TODO replace with real job logic
    }
}
