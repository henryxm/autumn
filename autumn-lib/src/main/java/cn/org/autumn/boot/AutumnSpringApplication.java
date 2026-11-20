package cn.org.autumn.boot;

import cn.org.autumn.config.JvmRestartHandler;
import cn.org.autumn.install.InstallJvmRestartCleanupCoordinator;
import cn.org.autumn.install.InstallRestartCoordinator;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.metrics.StartupStep;

/**
 * Autumn 应用入口封装：首次 {@link SpringApplication#run}，并在 {@code autumn.install.mode=true} 时
 * 循环处理安装向导触发的进程内重启（等待 {@link InstallRestartCoordinator}、JVM 清理、再次 run）。
 */
@Slf4j
public final class AutumnSpringApplication {

    private AutumnSpringApplication() {
    }

    /**
     * 与 {@link SpringApplication#run(Class, String[])} 等价；安装模式下会在当前 JVM 内多次启动直至
     * {@code autumn.install.mode} 变为 false。
     *
     * @param primarySource 通常为主类（带 {@code @SpringBootApplication}）
     * @param args          命令行参数
     * @return 最终存活的上下文；若等待重启过程中线程被中断则返回 {@code null}
     */
    public static ConfigurableApplicationContext run(Class<?> primarySource, String[] args) {
        ConfigurableApplicationContext context = runOnce(primarySource, args);
        Environment env = context.getEnvironment();
        while (Boolean.parseBoolean(env.getProperty("autumn.install.mode", "false"))) {
            try {
                context.getBean(InstallRestartCoordinator.class).awaitRestart();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            InstallJvmRestartCleanupCoordinator jvmCleanup = context.getBean(InstallJvmRestartCleanupCoordinator.class);
            List<JvmRestartHandler> restartCleaners = jvmCleanup.snapshotOrderedCleaners(context);
            SpringApplication.exit(context, () -> 0);
            jvmCleanup.invokeAllAfterContextClosed(restartCleaners);
            context = runOnce(primarySource, args);
            env = context.getEnvironment();
        }
        return context;
    }

    private static ConfigurableApplicationContext runOnce(Class<?> primarySource, String[] args) {
        SpringApplication app = new SpringApplication(primarySource);
        BufferingApplicationStartup startup = new BufferingApplicationStartup(4096);
        app.setApplicationStartup(startup);
        ConfigurableApplicationContext context = app.run(args);
        logStartupSteps(context.getEnvironment(), startup);
        return context;
    }

    /**
     * 输出 Spring {@code spring.beans.instantiate} 等步骤中 wall-time 最长的若干条（默认 Top 15，阈值 100ms）。
     */
    private static void logStartupSteps(Environment env, BufferingApplicationStartup startup) {
        if (!Boolean.parseBoolean(env.getProperty("autumn.startup.steps.enabled", "true"))) {
            return;
        }
        long thresholdMs = 100L;
        try {
            thresholdMs = Long.parseLong(env.getProperty("autumn.startup.steps.threshold-ms", "100").trim());
        } catch (NumberFormatException ignored) {
        }
        int top = 15;
        try {
            top = Integer.parseInt(env.getProperty("autumn.startup.steps.top", "15").trim());
        } catch (NumberFormatException ignored) {
        }
        List<StartupTimeline.TimelineEvent> events = new ArrayList<>(startup.getBufferedTimeline().getEvents());
        events.sort(Comparator.comparingLong((StartupTimeline.TimelineEvent e) ->
                e.getDuration() == null ? 0L : e.getDuration().toMillis()).reversed());
        StringBuilder sb = new StringBuilder(512);
        int shown = 0;
        for (StartupTimeline.TimelineEvent event : events) {
            long ms = event.getDuration() == null ? 0L : event.getDuration().toMillis();
            if (ms < thresholdMs || shown >= top) {
                continue;
            }
            if (shown > 0) {
                sb.append("; ");
            }
            String name = event.getStartupStep().getName();
            String beanTag = null;
            for (StartupStep.Tag tag : event.getStartupStep().getTags()) {
                if ("beanName".equals(tag.getKey())) {
                    beanTag = tag.getValue();
                    break;
                }
            }
            if (beanTag != null && !beanTag.isBlank()) {
                sb.append(name).append('[').append(beanTag).append(']');
            } else {
                sb.append(name);
            }
            sb.append('=').append(ms).append("ms");
            shown++;
        }
        if (shown == 0) {
            log.debug("Startup steps: none above thresholdMs={} (events={})", thresholdMs, events.size());
        } else {
            log.debug("Startup steps top{}(>= {}ms): {}", shown, thresholdMs, sb);
        }
    }
}
