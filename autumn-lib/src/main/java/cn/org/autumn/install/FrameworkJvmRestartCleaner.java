package cn.org.autumn.install;

import cn.org.autumn.config.JvmRestartHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 在更小 {@link Order} 值的 {@link JvmRestartHandler}（例如 MyBatis-Plus 表元数据清理）之后执行框架级 static 清理。
 */
@Component
public class FrameworkJvmRestartCleaner implements JvmRestartHandler {

    @Override
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public void cleanAfterContextClosed() {
        JvmRestartStaticStateReset.resetAll();
    }
}
