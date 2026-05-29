package cn.org.autumn.modules.safe.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 支付闸门令牌与免密窗口（Redis 优先，无 Redis 本机降级）。
 */
@Component
public class PayGateTokenStore implements LoopJob.OneMinute {

    private static final String REDIS_GATE_PREFIX = "safe:gate:";
    private static final String REDIS_PWDLESS_PREFIX = "safe:pwdless:";

    private final ConcurrentHashMap<String, GateEntry> localGate = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> localPwdlessUntil = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${autumn.redis.open:false}")
    private boolean redisOpen;

    @Autowired
    private SafeConfig safeConfig;

    public String issueGateToken(String userUuid, long amountCent, String orderId, String authMode) {
        PayCredentialConfig config = safeConfig.get();
        int minutes = config.getGateTokenMinutes() > 0 ? config.getGateTokenMinutes() : 5;
        String token = RandomStringUtils.randomAlphanumeric(32);
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
        GateEntry entry = new GateEntry(userUuid, amountCent, orderId, authMode, expireAt);
        if (redisOpen && redisTemplate != null) {
            redisTemplate.opsForValue().set(REDIS_GATE_PREFIX + token, entry.serialize(), minutes, TimeUnit.MINUTES);
            return token;
        }
        localGate.put(token, entry);
        return token;
    }

    public GateEntry consumeGateToken(String userUuid, String gateToken, long amountCent) throws CodeException {
        if (StringUtils.isBlank(gateToken))
            throw new CodeException(Error.PAY_GATE_TOKEN_INVALID);
        GateEntry entry = removeGateEntry(gateToken);
        if (entry == null || entry.expireAt < System.currentTimeMillis())
            throw new CodeException(Error.PAY_GATE_TOKEN_INVALID);
        if (!userUuid.equals(entry.userUuid))
            throw new CodeException(Error.PAY_GATE_TOKEN_INVALID);
        if (amountCent != entry.amountCent)
            throw new CodeException(Error.PAY_GATE_AMOUNT_MISMATCH);
        if (PayGateAttemptEntity.AUTH_DENIED.equals(entry.authMode))
            throw new CodeException(Error.PAY_GATE_DENIED);
        return entry;
    }

    public void markPasswordlessWindow(String userUuid, int windowMinutes) {
        if (windowMinutes <= 0)
            return;
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(windowMinutes);
        if (redisOpen && redisTemplate != null) {
            redisTemplate.opsForValue().set(REDIS_PWDLESS_PREFIX + userUuid, expireAt, windowMinutes, TimeUnit.MINUTES);
            return;
        }
        localPwdlessUntil.put(userUuid, expireAt);
    }

    public boolean isPasswordlessWindowActive(String userUuid) {
        return getPasswordlessRemainingSeconds(userUuid) > 0;
    }

    public long getPasswordlessRemainingSeconds(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return 0;
        long until = 0;
        if (redisOpen && redisTemplate != null) {
            Object val = redisTemplate.opsForValue().get(REDIS_PWDLESS_PREFIX + userUuid);
            if (val == null)
                return 0;
            try {
                until = Long.parseLong(String.valueOf(val));
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            Long local = localPwdlessUntil.get(userUuid);
            until = local == null ? 0 : local;
        }
        long remain = until - System.currentTimeMillis();
        return remain > 0 ? remain / 1000 : 0;
    }

    private GateEntry removeGateEntry(String gateToken) {
        if (redisOpen && redisTemplate != null) {
            String key = REDIS_GATE_PREFIX + gateToken;
            Object val = redisTemplate.opsForValue().get(key);
            if (val == null)
                return null;
            redisTemplate.delete(key);
            return GateEntry.deserialize(String.valueOf(val));
        }
        return localGate.remove(gateToken);
    }

    @Override
    public void onOneMinute() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, GateEntry>> it = localGate.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now)
                it.remove();
        }
        Iterator<Map.Entry<String, Long>> ip = localPwdlessUntil.entrySet().iterator();
        while (ip.hasNext()) {
            if (ip.next().getValue() < now)
                ip.remove();
        }
    }

    public static final class GateEntry {
        public final String userUuid;
        public final long amountCent;
        public final String orderId;
        public final String authMode;
        public final long expireAt;

        public GateEntry(String userUuid, long amountCent, String orderId, String authMode, long expireAt) {
            this.userUuid = userUuid;
            this.amountCent = amountCent;
            this.orderId = orderId == null ? "" : orderId;
            this.authMode = authMode;
            this.expireAt = expireAt;
        }

        String serialize() {
            return userUuid + "|" + amountCent + "|" + orderId + "|" + authMode + "|" + expireAt;
        }

        static GateEntry deserialize(String raw) {
            if (StringUtils.isBlank(raw))
                return null;
            String[] p = raw.split("\\|", 5);
            if (p.length < 5)
                return null;
            try {
                return new GateEntry(p[0], Long.parseLong(p[1]), p[2], p[3], Long.parseLong(p[4]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
