package cn.org.autumn.modules.install;

import cn.org.autumn.config.JvmRestartHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 清理 MyBatis-Plus 2.x 在 JVM 内保留的 static 缓存，避免安装阶段 H2（{@code MODE=MySQL}）与正式库（如 PostgreSQL）
 * 混用同一进程时，{@code TableInfo} 等元数据仍带 MySQL 反引号。
 *
 * @see MybatisPlusJvmCachesReset
 */
@Component
public class MybatisPlusInstallJvmRestartCleaner implements JvmRestartHandler {

    @Override
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void cleanAfterContextClosed() {
        MybatisPlusJvmCachesReset.clearForProcessRestart();
    }
}
