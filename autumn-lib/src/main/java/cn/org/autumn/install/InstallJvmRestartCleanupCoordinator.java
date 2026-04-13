package cn.org.autumn.install;

import cn.org.autumn.site.Factory;
import cn.org.autumn.config.JvmRestartHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 安装向导等场景下进程内重启：在 {@code SpringApplication.exit} 之前快照已排序的
 * {@link JvmRestartHandler}，在 {@code exit} 之后依次调用。
 */
@Slf4j
@Component
public class InstallJvmRestartCleanupCoordinator {

    private static final String CLEAN_METHOD = "cleanAfterContextClosed";

    /**
     * 须在仍存活的 {@link ApplicationContext} 上调用，用于 {@link org.springframework.boot.SpringApplication#exit} 之前。
     */
    public List<JvmRestartHandler> snapshotOrderedCleaners(ApplicationContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        return Factory.getOrderList(context, JvmRestartHandler.class, CLEAN_METHOD);
    }

    /**
     * 须在 {@code SpringApplication.exit} 之后调用。
     */
    public void invokeAllAfterContextClosed(List<JvmRestartHandler> cleaners) {
        if (cleaners == null || cleaners.isEmpty()) {
            return;
        }
        for (JvmRestartHandler cleaner : cleaners) {
            if (cleaner == null) {
                continue;
            }
            try {
                cleaner.cleanAfterContextClosed();
            } catch (Throwable t) {
                log.warn("InstallJvmRestartCleaner [{}] 执行失败: {}", cleaner.getClass().getName(), t.toString(), t);
            }
        }
    }
}
