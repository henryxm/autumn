package cn.org.autumn.thread;

import cn.org.autumn.config.Config;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class LockOnce extends TagRunnable {

    Logger log = LoggerFactory.getLogger(LockOnce.class);

    private static RedissonClient redissonClient;

    @Override
    public void run() {
        if (can()) {
            if (null == redissonClient)
                redissonClient = (RedissonClient) Config.getBean(RedissonClient.class);
            if (null != redissonClient && null != getTagValue()) {
                TagValue value = getTagValue();
                String id = "loopjob:lock:" + value.type().getSimpleName() + ":" + value.method();
                RLock lock = redissonClient.getLock(id);
                try {
                    if (!lock.isLocked()) {
                        boolean isLocked = lock.tryLock(0, value.time(), TimeUnit.MINUTES);
                        if (isLocked) {
                            log.debug("锁定任务:{}, ID:{}", value.tag(), id);
                            super.run();
                        }
                    }
                } catch (Exception e) {
                    log.error("锁定任务:{}", e.getMessage());
                } finally {
                    if (null != lock && lock.isHeldByCurrentThread())
                        lock.unlock();
                }
            } else {
                super.run();
            }
        }
        TagTaskExecutor.remove(LockOnce.this);
    }
}
