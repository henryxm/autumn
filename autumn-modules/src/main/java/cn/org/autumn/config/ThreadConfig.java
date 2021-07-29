package cn.org.autumn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadConfig {
    /**
     * 自定义异步线程池
     *
     * @return AsyncTaskExecutor
     */
    @Bean
    public AsyncTaskExecutor asyncTaskExecutor(List<RejectedHandler> rejectedHandlers) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("Anno-Executor");
        executor.setMaxPoolSize(50000);
        executor.setCorePoolSize(1000);
        if (null != rejectedHandlers && rejectedHandlers.size() > 0) {
            // 设置拒绝策略
            executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    for (RejectedHandler rejectedHandler : rejectedHandlers) {
                        rejectedHandler.rejectedExecution(r, executor);
                    }
                }
            });
        } else {
            // 使用预定义的异常处理类
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return executor;
    }
}