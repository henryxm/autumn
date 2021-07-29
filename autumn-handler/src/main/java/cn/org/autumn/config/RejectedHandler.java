package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Component
@ConditionalOnMissingBean(RejectedHandler.class)
public interface RejectedHandler {
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}