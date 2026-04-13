package cn.org.autumn.boot;

import cn.org.autumn.install.InstallMode;
import cn.org.autumn.modules.job.task.LoopJob;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 安装向导（{@code autumn.install.mode=true}）下收敛后台调度，避免访问未就绪的中间件或业务库。
 * <ul>
 *     <li>{@link LoopJob#pauseAll()}：{@code LoopJob} 在 {@code executeJob} 入口检查全局暂停。</li>
 *     <li>Quartz {@link Scheduler#standby()}：暂停库表等配置的定时任务（与 LoopJob 触发链解耦时的兜底）。</li>
 * </ul>
 * 正常启动时 {@link InstallMode} 为 false，不执行任何操作。安装完成重启进程后无需 {@link LoopJob#resumeAll()}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InstallModeBackgroundServices implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InstallModeBackgroundServices.class);

    private final ApplicationContext applicationContext;

    public InstallModeBackgroundServices(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!InstallMode.isActive()) {
            return;
        }
        LoopJob.pauseAll();
        log.info("Install mode: LoopJob.pauseAll() applied; periodic LoopJob callbacks will not run");
        pauseAllQuartzSchedulers();
    }

    private void pauseAllQuartzSchedulers() {
        Map<String, Scheduler> schedulers = applicationContext.getBeansOfType(Scheduler.class);
        if (schedulers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Scheduler> e : schedulers.entrySet()) {
            Scheduler scheduler = e.getValue();
            if (scheduler == null) {
                continue;
            }
            try {
                if (!scheduler.isInStandbyMode()) {
                    scheduler.standby();
                    log.info("Install mode: Quartz Scheduler [{}] set to standby", e.getKey());
                }
            } catch (SchedulerException ex) {
                log.warn("Install mode: Quartz Scheduler [{}] standby failed: {}", e.getKey(), ex.getMessage());
            }
        }
    }
}
