package cn.org.autumn.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(RejectedHandler.class)
public interface RejectedHandler {
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}
