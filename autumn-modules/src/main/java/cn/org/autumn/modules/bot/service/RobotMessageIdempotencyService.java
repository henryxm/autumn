package cn.org.autumn.modules.bot.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.model.RobotQuotaConfig;
import cn.org.autumn.modules.bot.dto.RobotMessagePushResult;
import cn.org.autumn.service.CacheService;
import cn.org.autumn.service.DistributedLockService;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 入站消息幂等：相同机器人 + 幂等键在缓存有效期内只入队一次。
 */
@Service
public class RobotMessageIdempotencyService {

    private static final CacheConfig IDEMPOTENCY_CACHE = CacheConfig.builder()
            .name("bot_message_idempotency")
            .key(String.class)
            .value(String.class)
            .expire(60)
            .unit(TimeUnit.MINUTES)
            .redis(24 * 60)
            .max(50000)
            .build();

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private Gson gson;

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    public RobotMessagePushResult replayIfPresent(String robotUuid, String idempotencyKey) {
        if (StringUtils.isBlank(idempotencyKey))
            return null;
        String cached = cacheService.get(IDEMPOTENCY_CACHE, cacheKey(robotUuid, idempotencyKey));
        if (StringUtils.isBlank(cached))
            return null;
        try {
            RobotMessagePushResult result = gson.fromJson(cached, RobotMessagePushResult.class);
            if (result != null)
                result.setDuplicate(true);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public RobotMessagePushResult executeOnce(String robotUuid, String idempotencyKey,
                                              java.util.concurrent.Callable<RobotMessagePushResult> action) throws Exception {
        if (StringUtils.isBlank(idempotencyKey))
            return action.call();
        String lockKey = "bot:msg:idem:" + cacheKey(robotUuid, idempotencyKey);
        return distributedLockService.withLock(lockKey, () -> {
            RobotMessagePushResult existing = replayIfPresent(robotUuid, idempotencyKey);
            if (existing != null)
                return existing;
            RobotMessagePushResult result = action.call();
            remember(robotUuid, idempotencyKey, result);
            return result;
        });
    }

    public void remember(String robotUuid, String idempotencyKey, RobotMessagePushResult result) {
        if (StringUtils.isBlank(idempotencyKey) || result == null)
            return;
        RobotQuotaConfig config = robotQuotaService.getGlobal();
        int hours = config == null ? 24 : config.getMessageIdempotencyHours();
        CacheConfig cacheConfig = IDEMPOTENCY_CACHE.toBuilder()
                .expire(hours * 60L)
                .redis(hours * 60L * 2)
                .build();
        cacheService.compute(cacheKey(robotUuid, idempotencyKey), () -> gson.toJson(result), cacheConfig);
    }

    private static String cacheKey(String robotUuid, String idempotencyKey) {
        return robotUuid + ":" + idempotencyKey.trim();
    }
}
