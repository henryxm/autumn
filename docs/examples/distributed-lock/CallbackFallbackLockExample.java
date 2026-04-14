package cn.org.autumn.examples.distributedlock;

import cn.org.autumn.service.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 示例：非继承链组件（回调处理器）三种加锁方式。
 */
@Slf4j
@Component
public class CallbackFallbackLockExample {

    @Autowired
    private DistributedLockService distributedLockService;

    public void onCallback(String bizNo) {
        // 默认使用 fallback，避免回调风暴下阻塞调用链
        onCallbackFallback(bizNo);
    }

    public void onCallbackStrict(String bizNo) {
        final String lockKey = "callback:biz:" + bizNo;
        distributedLockService.withLockUnchecked(lockKey, () -> processCallback(bizNo));
    }

    public void onCallbackFallback(String bizNo) {
        final String lockKey = "callback:biz:" + bizNo;
        distributedLockService.withLockOrFallbackUnchecked(lockKey, () -> {
            processCallback(bizNo);
            return null;
        }, (key, ex) -> {
            // 回调竞争失败的降级处理：记录后等待后续补偿
            log.warn("callback degraded bizNo={} key={} err={}", bizNo, key, ex.toString());
            return null;
        });
    }

    public void onCallbackRetry(String bizNo) {
        final String lockKey = "callback:biz:" + bizNo;
        distributedLockService.withLockRetryUnchecked(lockKey, () -> {
            processCallback(bizNo);
            return null;
        });
    }

    private void processCallback(String bizNo) {
        // TODO replace with real callback logic
    }
}
