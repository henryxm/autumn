package cn.org.autumn.modules.safe.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.job.task.LoopJob;
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
 * 短期 verifyToken 与 biometric challenge（Redis 优先，无 Redis 本机降级）。
 */
@Component
public class PayCredentialTokenStore implements LoopJob.OneMinute {

    private static final String REDIS_VERIFY_PREFIX = "safe:verify:";
    private static final String REDIS_CHALLENGE_PREFIX = "safe:challenge:";

    private final ConcurrentHashMap<String, VerifyEntry> localVerify = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChallengeEntry> localChallenge = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${autumn.redis.open:false}")
    private boolean redisOpen;

    @Autowired
    private SafeConfig safeConfig;

    public String issueVerifyToken(String userUuid, String method, long amountCent, String orderId) {
        PayCredentialConfig config = safeConfig.get();
        int minutes = config.getVerifyTokenMinutes() > 0 ? config.getVerifyTokenMinutes() : 5;
        String token = RandomStringUtils.randomAlphanumeric(32);
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
        VerifyEntry entry = new VerifyEntry(userUuid, method, amountCent, orderId == null ? "" : orderId, expireAt);
        if (redisOpen && redisTemplate != null) {
            String key = REDIS_VERIFY_PREFIX + token;
            redisTemplate.opsForValue().set(key, entry.serialize(), minutes, TimeUnit.MINUTES);
            return token;
        }
        localVerify.put(token, entry);
        return token;
    }

    public void consumeVerifyToken(String userUuid, String token) throws CodeException {
        consumeVerifyToken(userUuid, token, 0L, null);
    }

    public void consumeVerifyToken(String userUuid, String token, long amountCent, String orderId) throws CodeException {
        if (StringUtils.isBlank(token))
            throw new CodeException(Error.PAY_VERIFY_TOKEN_INVALID);
        PayCredentialConfig config = safeConfig.get();
        VerifyEntry entry = removeVerifyEntry(token);
        if (entry == null || entry.expireAt < System.currentTimeMillis())
            throw new CodeException(Error.PAY_VERIFY_TOKEN_INVALID);
        if (!userUuid.equals(entry.userUuid))
            throw new CodeException(Error.PAY_VERIFY_TOKEN_INVALID);
        if (config.isVerifyTokenBindAmount()) {
            if (amountCent > 0 && entry.amountCent > 0 && amountCent != entry.amountCent)
                throw new CodeException(Error.PAY_GATE_AMOUNT_MISMATCH);
            if (StringUtils.isNotBlank(orderId) && StringUtils.isNotBlank(entry.orderId) && !orderId.equals(entry.orderId))
                throw new CodeException(Error.PAY_GATE_AMOUNT_MISMATCH);
        }
    }

    public String issueChallenge(String userUuid, String deviceId) {
        PayCredentialConfig config = safeConfig.get();
        int minutes = config.getChallengeMinutes() > 0 ? config.getChallengeMinutes() : 5;
        String challenge = RandomStringUtils.randomAlphanumeric(32);
        String composite = userUuid + ":" + deviceId;
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
        if (redisOpen && redisTemplate != null) {
            String key = REDIS_CHALLENGE_PREFIX + composite;
            redisTemplate.opsForValue().set(key, challenge, minutes, TimeUnit.MINUTES);
            return challenge;
        }
        localChallenge.put(composite, new ChallengeEntry(challenge, expireAt));
        return challenge;
    }

    public String requireChallenge(String userUuid, String deviceId, String challenge) throws CodeException {
        String composite = userUuid + ":" + deviceId;
        if (redisOpen && redisTemplate != null) {
            String key = REDIS_CHALLENGE_PREFIX + composite;
            Object val = redisTemplate.opsForValue().get(key);
            if (val == null || !challenge.equals(String.valueOf(val)))
                throw new CodeException(Error.PAY_BIOMETRIC_VERIFY_FAILED);
            redisTemplate.delete(key);
            return challenge;
        }
        ChallengeEntry entry = localChallenge.remove(composite);
        if (entry == null || entry.expireAt < System.currentTimeMillis() || !challenge.equals(entry.challenge))
            throw new CodeException(Error.PAY_BIOMETRIC_VERIFY_FAILED);
        return challenge;
    }

    @Override
    public void onOneMinute() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, VerifyEntry>> itv = localVerify.entrySet().iterator();
        while (itv.hasNext()) {
            if (itv.next().getValue().expireAt < now)
                itv.remove();
        }
        Iterator<Map.Entry<String, ChallengeEntry>> itc = localChallenge.entrySet().iterator();
        while (itc.hasNext()) {
            if (itc.next().getValue().expireAt < now)
                itc.remove();
        }
    }

    private VerifyEntry removeVerifyEntry(String token) {
        if (redisOpen && redisTemplate != null) {
            String key = REDIS_VERIFY_PREFIX + token;
            Object val = redisTemplate.opsForValue().get(key);
            if (val == null)
                return null;
            redisTemplate.delete(key);
            return VerifyEntry.deserialize(String.valueOf(val));
        }
        return localVerify.remove(token);
    }

    static final class VerifyEntry {
        final String userUuid;
        final String method;
        final long amountCent;
        final String orderId;
        final long expireAt;

        VerifyEntry(String userUuid, String method, long amountCent, String orderId, long expireAt) {
            this.userUuid = userUuid;
            this.method = method == null ? "" : method;
            this.amountCent = amountCent;
            this.orderId = orderId;
            this.expireAt = expireAt;
        }

        String serialize() {
            return userUuid + "|" + method + "|" + amountCent + "|" + orderId + "|" + expireAt;
        }

        static VerifyEntry deserialize(String raw) {
            if (StringUtils.isBlank(raw))
                return null;
            String[] p = raw.split("\\|", 5);
            if (p.length < 5)
                return null;
            try {
                return new VerifyEntry(p[0], p[1], Long.parseLong(p[2]), p[3], Long.parseLong(p[4]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static final class ChallengeEntry {
        private final String challenge;
        private final long expireAt;

        private ChallengeEntry(String challenge, long expireAt) {
            this.challenge = challenge;
            this.expireAt = expireAt;
        }
    }
}
