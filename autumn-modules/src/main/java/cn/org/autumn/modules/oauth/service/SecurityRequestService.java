package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.oauth.dao.SecurityRequestDao;
import cn.org.autumn.modules.oauth.entity.SecurityRequestEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityRequestService extends ModuleService<SecurityRequestDao, SecurityRequestEntity> implements LoopJob.OneDay {

    private static final int ROTATE_DAYS = 7;
    private static final int KEEP_DAYS = 30;
    public static final String AUTH_HEADER = "X-Encrypt-Auth";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String AGENT_HEADER = "X-Encrypt-Agent";
    public static final String TIMESTAMP_HEADER = "X-Encrypt-Timestamp";
    public static final String NONCE_HEADER = "X-Encrypt-Nonce";
    public static final String SIGNATURE_HEADER = "X-Encrypt-Signature";
    private static final long MAX_SKEW_MILLIS = 120000L;
    private static final long NONCE_TTL_MILLIS = 120000L;
    private static final int NONCE_GC_THRESHOLD = 10000;
    private static final Map<String, Long> NONCE_REPLAY_GUARD = new ConcurrentHashMap<>();

    public synchronized SecurityRequestEntity ensureCurrent() {
        SecurityRequestEntity latest = baseMapper.getLatestEnabled();
        if (latest == null) {
            return createNew();
        }
        Date now = new Date();
        Date rotateBefore = offsetDay(now, -ROTATE_DAYS);
        if (latest.getCreate() == null || latest.getCreate().before(rotateBefore)) {
            return createNew();
        }
        return latest;
    }

    public boolean verify(String userAgent, String auth) {
        return verify(userAgent, null, auth);
    }

    public boolean verify(String userAgent, String agentHeader, String auth) {
        SecurityRequestEntity request = getEnabledByAuth(auth);
        if (request == null || !request.isEnabled() || StringUtils.isBlank(request.getAgent())) {
            return false;
        }
        return verifyAgent(request, userAgent, agentHeader);
    }

    public boolean verifyStrong(String userAgent, String agentHeader, String auth,
                                String method, String uri,
                                String timestampHeader, String nonce, String signature) {
        SecurityRequestEntity request = getEnabledByAuth(auth);
        if (request == null || !request.isEnabled() || StringUtils.isBlank(request.getAgent())) {
            return false;
        }
        if (!verifyAgent(request, userAgent, agentHeader)) {
            return false;
        }
        if (StringUtils.isBlank(method) || StringUtils.isBlank(uri)
                || StringUtils.isBlank(timestampHeader)
                || StringUtils.isBlank(nonce)
                || StringUtils.isBlank(signature)) {
            return false;
        }
        long timestamp = parseTimestamp(timestampHeader);
        if (timestamp <= 0L) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > MAX_SKEW_MILLIS) {
            return false;
        }
        if (!registerNonceOnce(auth, nonce, now)) {
            return false;
        }
        String canonical = method.toUpperCase() + "\n"
                + uri + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + request.getAgent();
        String expect = signHmacSha256(auth, canonical);
        return StringUtils.equalsIgnoreCase(expect, signature);
    }

    private SecurityRequestEntity getEnabledByAuth(String auth) {
        if (StringUtils.isBlank(auth)) {
            return null;
        }
        SecurityRequestEntity request = baseMapper.getByAuth(auth);
        if (request == null || !request.isEnabled() || StringUtils.isBlank(request.getAgent())) {
            return null;
        }
        return request;
    }

    private boolean verifyAgent(SecurityRequestEntity request, String userAgent, String agentHeader) {
        String agent = request.getAgent();
        if (StringUtils.isNotBlank(userAgent) && userAgent.contains(agent)) {
            return true;
        }
        // 浏览器端通常不能稳定修改User-Agent，提供备用特征头兜底
        return StringUtils.isNotBlank(agentHeader) && agent.equals(agentHeader);
    }

    private long parseTimestamp(String timestampHeader) {
        try {
            long value = Long.parseLong(timestampHeader.trim());
            if (value > 0 && value < 1000000000000L) {
                return value * 1000L;
            }
            return value;
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private boolean registerNonceOnce(String auth, String nonce, long now) {
        if (StringUtils.isBlank(nonce) || nonce.length() > 128) {
            return false;
        }
        if (NONCE_REPLAY_GUARD.size() > NONCE_GC_THRESHOLD) {
            gcNonce(now);
        }
        String key = auth + ":" + nonce;
        Long deadline = NONCE_REPLAY_GUARD.get(key);
        if (deadline != null && deadline > now) {
            return false;
        }
        NONCE_REPLAY_GUARD.put(key, now + NONCE_TTL_MILLIS);
        return true;
    }

    private void gcNonce(long now) {
        Iterator<Map.Entry<String, Long>> iterator = NONCE_REPLAY_GUARD.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long deadline = entry.getValue();
            if (deadline == null || deadline <= now) {
                iterator.remove();
            }
        }
    }

    private String signHmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sign = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(sign.length * 2);
            for (byte b : sign) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public SecurityRequestEntity createNew() {
        Date now = new Date();
        SecurityRequestEntity entity = new SecurityRequestEntity();
        entity.setAgent("AUTUMN-AGENT-" + Uuid.uuid().replace("-", ""));
        entity.setAuth("AUTUMN-AUTH-" + Uuid.uuid().replace("-", ""));
        entity.setEnabled(true);
        entity.setCreate(now);
        entity.setExpire(offsetDay(now, ROTATE_DAYS));
        insert(entity);
        return entity;
    }

    public int cleanupExpired() {
        return baseMapper.deleteByCreateBefore(offsetDay(new Date(), -KEEP_DAYS));
    }

    @Override
    public void onOneDay() {
        ensureCurrent();
        cleanupExpired();
    }

    @Override
    public void init() {
        super.init();
        ensureCurrent();
        cleanupExpired();
    }

    private Date offsetDay(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

}
