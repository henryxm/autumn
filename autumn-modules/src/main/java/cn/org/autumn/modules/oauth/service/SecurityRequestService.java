package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.oauth.dao.SecurityRequestDao;
import cn.org.autumn.modules.oauth.entity.SecurityRequestEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.utils.Uuid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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
    private static final int VERIFY_LOG_ROLLING_LIMIT = 100;
    private static final Deque<Map<String, Object>> VERIFY_STRONG_LOGS = new ConcurrentLinkedDeque<>();

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
        if (request == null || request.getEnabled() == 0 || StringUtils.isBlank(request.getAgent())) {
            return false;
        }
        return verifyAgent(request, userAgent, agentHeader);
    }

    public boolean verifyStrong(String userAgent, String agentHeader, String auth, String method, String uri, String timestampHeader, String nonce, String signature) {
        long now = System.currentTimeMillis();
        SecurityRequestEntity request = getEnabledByAuth(auth);
        boolean userAgentMatched = false;
        boolean agentHeaderMatched = false;
        long skew = -1L;
        boolean nonceAccepted = false;
        boolean signatureMatched = false;
        String reason = "ok";
        if (log.isDebugEnabled()) {
            log.debug("verifyStrong.start auth={}, method={}, uri={}, ts={}, nonce={}, sign={}, ua={}, agentHeader={}", mask(auth), method, uri, timestampHeader, mask(nonce), mask(signature), brief(userAgent), mask(agentHeader));
            log.debug("verifyStrong.system requestFound={}, requestEnabled={}, expectedAgent={}", request != null, request != null && request.getEnabled() != 0, request == null ? "-" : mask(request.getAgent()));
        }
        if (request == null || request.getEnabled() == 0 || StringUtils.isBlank(request.getAgent())) {
            reason = "auth_or_request_invalid";
            if (log.isDebugEnabled()) {
                log.debug("verifyStrong.fail reason=auth_or_request_invalid auth={}", mask(auth));
            }
            appendVerifyStrongLog(now, false, reason, auth, method, uri, userAgent, agentHeader, null, userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
            return false;
        }
        userAgentMatched = StringUtils.isNotBlank(userAgent) && userAgent.contains(request.getAgent());
        agentHeaderMatched = StringUtils.isNotBlank(agentHeader) && request.getAgent().equals(agentHeader);
        if (log.isDebugEnabled()) {
            log.debug("verifyStrong.agentCompare uaContainsExpected={}, headerEqualsExpected={}, expectedAgent={}, requestAgentHeader={}", userAgentMatched, agentHeaderMatched, mask(request.getAgent()), mask(agentHeader));
        }
        if (!(userAgentMatched || agentHeaderMatched)) {
            reason = "agent_mismatch";
            if (log.isDebugEnabled()) {
                log.debug("verifyStrong.fail reason=agent_mismatch expectedAgent={}, ua={}, agentHeader={}", mask(request.getAgent()), brief(userAgent), mask(agentHeader));
            }
            appendVerifyStrongLog(now, false, reason, auth, method, uri, userAgent, agentHeader, request.getAgent(), userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
            return false;
        }
        if (StringUtils.isBlank(method) || StringUtils.isBlank(uri)
                || StringUtils.isBlank(timestampHeader)
                || StringUtils.isBlank(nonce)
                || StringUtils.isBlank(signature)) {
            reason = "required_field_missing";
            if (log.isDebugEnabled()) {
                log.debug("verifyStrong.fail reason=required_field_missing methodBlank={}, uriBlank={}, tsBlank={}, nonceBlank={}, signBlank={}", StringUtils.isBlank(method), StringUtils.isBlank(uri), StringUtils.isBlank(timestampHeader), StringUtils.isBlank(nonce), StringUtils.isBlank(signature));
            }
            appendVerifyStrongLog(now, false, reason, auth, method, uri, userAgent, agentHeader, request.getAgent(), userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
            return false;
        }
        long timestamp = parseTimestamp(timestampHeader);
        if (timestamp <= 0L) {
            reason = "timestamp_invalid";
            if (log.isDebugEnabled()) {
                log.debug("verifyStrong.fail reason=timestamp_invalid rawTs={}", timestampHeader);
            }
            appendVerifyStrongLog(now, false, reason, auth, method, uri, userAgent, agentHeader, request.getAgent(), userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
            return false;
        }
        skew = Math.abs(now - timestamp);
        if (log.isDebugEnabled()) {
            log.debug("verifyStrong.timeCompare now={}, requestTs={}, skewMs={}, maxSkewMs={}", now, timestamp, skew, MAX_SKEW_MILLIS);
        }
        if (skew > MAX_SKEW_MILLIS) {
            reason = "timestamp_skew_exceeded";
            if (log.isDebugEnabled()) {
                log.debug("verifyStrong.fail reason=timestamp_skew_exceeded skewMs={}, maxSkewMs={}", skew, MAX_SKEW_MILLIS);
            }
            appendVerifyStrongLog(now, false, reason, auth, method, uri, userAgent, agentHeader, request.getAgent(), userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
            return false;
        }
        nonceAccepted = registerNonceOnce(auth, nonce, now);
        if (log.isDebugEnabled()) {
            log.debug("verifyStrong.nonceCompare accepted={}, nonce={}", nonceAccepted, mask(nonce));
        }
        if (!nonceAccepted) {
            reason = "nonce_replay_or_invalid";
            if (log.isDebugEnabled()) {
                log.debug("verifyStrong.fail reason=nonce_replay_or_invalid nonce={}", mask(nonce));
            }
            appendVerifyStrongLog(now, false, reason, auth, method, uri, userAgent, agentHeader, request.getAgent(), userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
            return false;
        }
        String canonical = method.toUpperCase() + "\n"
                + uri + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + request.getAgent();
        String expect = signHmacSha256(auth, canonical);
        signatureMatched = StringUtils.equalsIgnoreCase(expect, signature);
        reason = signatureMatched ? "ok" : "signature_not_match";
        if (log.isDebugEnabled()) {
            log.debug("verifyStrong.signatureCompare matched={}, expected={}, request={}", signatureMatched, mask(expect), mask(signature));
        }
        appendVerifyStrongLog(now, signatureMatched, reason, auth, method, uri, userAgent, agentHeader, request.getAgent(), userAgentMatched, agentHeaderMatched, skew, nonceAccepted, signatureMatched);
        return signatureMatched;
    }

    public List<Map<String, Object>> getVerifyStrongLogs(int limit) {
        int size = limit <= 0 ? VERIFY_LOG_ROLLING_LIMIT : Math.min(limit, VERIFY_LOG_ROLLING_LIMIT);
        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> item : VERIFY_STRONG_LOGS) {
            result.add(item);
            i++;
            if (i >= size) {
                break;
            }
        }
        return result;
    }

    public void clearVerifyStrongLogs() {
        VERIFY_STRONG_LOGS.clear();
    }

    private SecurityRequestEntity getEnabledByAuth(String auth) {
        if (StringUtils.isBlank(auth)) {
            return null;
        }
        SecurityRequestEntity request = baseMapper.getByAuth(auth);
        if (request == null || request.getEnabled() == 0 || StringUtils.isBlank(request.getAgent())) {
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
        entity.setAgent("," + Uuid.uuid().replace("-", "").substring(0, 8));
        entity.setAuth("x-auth-" + Uuid.uuid().replace("-", ""));
        entity.setEnabled(1);
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

    private void appendVerifyStrongLog(long now,
                                       boolean pass,
                                       String reason,
                                       String auth,
                                       String method,
                                       String uri,
                                       String userAgent,
                                       String agentHeader,
                                       String expectedAgent,
                                       boolean userAgentMatched,
                                       boolean agentHeaderMatched,
                                       long skew,
                                       boolean nonceAccepted,
                                       boolean signatureMatched) {
        Map<String, Object> item = new HashMap<>();
        item.put("time", now);
        item.put("pass", pass);
        item.put("reason", reason);
        item.put("auth", mask(auth));
        item.put("method", StringUtils.defaultString(method, "-"));
        item.put("uri", StringUtils.defaultString(uri, "-"));
        item.put("userAgent", brief(userAgent));
        item.put("agentHeader", mask(agentHeader));
        item.put("expectedAgent", mask(expectedAgent));
        item.put("userAgentMatched", userAgentMatched);
        item.put("agentHeaderMatched", agentHeaderMatched);
        item.put("skewMs", skew);
        item.put("nonceAccepted", nonceAccepted);
        item.put("signatureMatched", signatureMatched);
        VERIFY_STRONG_LOGS.addFirst(item);
        while (VERIFY_STRONG_LOGS.size() > VERIFY_LOG_ROLLING_LIMIT) {
            VERIFY_STRONG_LOGS.pollLast();
        }
    }

    private String mask(String value) {
        if (StringUtils.isBlank(value)) {
            return "-";
        }
        int keep = Math.min(8, value.length());
        return value.substring(0, keep) + "...(len=" + value.length() + ")";
    }

    private String brief(String value) {
        if (StringUtils.isBlank(value)) {
            return "-";
        }
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }
}