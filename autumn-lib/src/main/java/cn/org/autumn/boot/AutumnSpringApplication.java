package cn.org.autumn.boot;

import cn.org.autumn.install.InstallJvmRestartCleanupCoordinator;
import cn.org.autumn.install.InstallRestartCoordinator;
import cn.org.autumn.config.JvmRestartHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Autumn 应用入口封装：首次 {@link SpringApplication#run}，并在 {@code autumn.install.mode=true} 时
 * 循环处理安装向导触发的进程内重启（等待 {@link InstallRestartCoordinator}、JVM 清理、再次 run）。
 */
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
        ConfigurableApplicationContext context = SpringApplication.run(primarySource, args);
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
            context = SpringApplication.run(primarySource, args);
            env = context.getEnvironment();
        }
        return context;
    }
}
