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

    public LockOnce() {
    }

    public LockOnce(String id) {
        super(id);
    }

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
                    boolean isLocked = lock.tryLock();
                    if (isLocked) {
                        log.debug("锁定任务:{}, ID:{}", value.tag(), id);
                        //Sleep for five second to avoid return immediately
                        Thread.sleep(5000);
                        super.run();
                    } else {
                        log.debug("未锁定任务:{}, ID:{}", value.tag(), id);
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
