package cn.org.autumn.modules.ops.service;

import cn.org.autumn.config.GsonConfig;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.AES;
import cn.org.autumn.utils.Base64Texts;
import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.RsaUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运维 AES 会话与挑战（独立于业务 rsa 会话）。
 * 有 Redis 时短 TTL 共享；否则本机内存（单机）。
 */
@Slf4j
@Service
public class OpsSessionService {

    private static final long CHALLENGE_TTL_MS = 120_000L;
    private static final long SESSION_TTL_MS = 30 * 60_000L;
    private static final long CHALLENGE_TTL_SEC = 120L;
    private static final long SESSION_TTL_SEC = 30 * 60L;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Challenge> localChallenges = new ConcurrentHashMap<>();
    private final Map<String, OpsSession> localSessions = new ConcurrentHashMap<>();

    @Autowired
    private OpsCertService opsCertService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired(required = false)
    private RedisUtils redisUtils;

    public String createChallenge(String certId) {
        OpsClientCertEntry client = opsCertService.findActive(certId);
        if (client == null) {
            throw new AException("证书无效或已吊销", 403);
        }
        byte[] raw = new byte[24];
        secureRandom.nextBytes(raw);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Challenge ch = new Challenge(certId, System.currentTimeMillis() + CHALLENGE_TTL_MS);
        if (redisOpen()) {
            redisUtils.set(challengeKey(nonce), GsonConfig.getGson().toJson(ch), CHALLENGE_TTL_SEC);
        } else {
            localChallenges.put(nonce, ch);
        }
        return nonce;
    }

    /**
     * @param aesKeyWrappedByServerPub 客户端生成的 AES key(Base64) 用服务端公钥 RSA 加密后的密文
     * @param aesIvBase64              AES IV Base64（可空）
     * @param nonceSignature           客户端私钥对 nonce 的 SHA256withRSA 签名（Base64）
     */
    public OpsSession openSession(String certId, String nonce, String nonceSignature,
                                  String aesKeyWrappedByServerPub, String aesIvBase64) {
        Challenge ch = takeChallenge(nonce);
        if (ch == null || ch.expireAt < System.currentTimeMillis()) {
            throw new AException("挑战已失效", 403);
        }
        if (!certId.equals(ch.certId)) {
            throw new AException("证书与挑战不匹配", 403);
        }
        OpsClientCertEntry client = opsCertService.findActive(certId);
        if (client == null) {
            throw new AException("证书无效或已吊销", 403);
        }
        if (!verifySignature(client.getClientPublicKey(), nonce, nonceSignature)) {
            log.warn("SecurityAlert denied reason=ops_session detail=invalid_signature certId={}", certId);
            throw new AException("签名无效", 403);
        }
        SecurityOpsCertConfig cfg = opsCertService.getConfig();
        String aesKeyB64;
        try {
            aesKeyB64 = RsaUtil.decrypt(aesKeyWrappedByServerPub, cfg.getServerPrivateKey());
        } catch (Exception e) {
            throw new AException("AES 密钥解密失败", 403);
        }
        if (StringUtils.isBlank(aesKeyB64)) {
            throw new AException("AES 密钥无效", 403);
        }
        String sessionId = "opss-" + UUID.randomUUID().toString().replace("-", "");
        OpsSession session = new OpsSession(sessionId, certId, aesKeyB64,
                StringUtils.defaultString(aesIvBase64), System.currentTimeMillis() + SESSION_TTL_MS);
        putSession(session);
        opsCertService.touch(certId);
        return session;
    }

    public OpsSession requireSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            throw new AException("缺少运维会话", 401);
        }
        OpsSession s = getSession(sessionId);
        if (s == null || s.expireAt < System.currentTimeMillis()) {
            if (s != null) {
                removeSession(sessionId);
            }
            throw new AException("运维会话已失效", 401);
        }
        return s;
    }

    public String encryptForClient(OpsSession session, String plainJson) {
        try {
            return AES.encrypt(plainJson, session.aesKeyBase64, session.aesIvBase64);
        } catch (Exception e) {
            throw new AException("响应加密失败", 500);
        }
    }

    public String decryptFromClient(OpsSession session, String ciphertext) {
        try {
            return AES.decrypt(ciphertext, session.aesKeyBase64, session.aesIvBase64);
        } catch (Exception e) {
            throw new AException("请求解密失败", 400);
        }
    }

    public static boolean verifySignature(String publicKeyBase64, String payload, String signatureBase64) {
        try {
            byte[] keyBytes = Base64Texts.decode(stripPem(publicKeyBase64));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(spec);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64Texts.decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    public static String sign(String privateKeyBase64, String payload) {
        try {
            byte[] keyBytes = Base64Texts.decode(stripPem(privateKeyBase64));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey pri = kf.generatePrivate(spec);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(pri);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new AException("签名失败", 500);
        }
    }

    private boolean redisOpen() {
        return redisUtils != null && redisUtils.isOpen();
    }

    private String ns() {
        try {
            String n = sysConfigService.getNameSpace();
            return n != null ? n : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String challengeKey(String nonce) {
        return RedisKeys.getOpsChallengeKey(ns(), nonce);
    }

    private String sessionKey(String sessionId) {
        return RedisKeys.getOpsSessionKey(ns(), sessionId);
    }

    private Challenge takeChallenge(String nonce) {
        if (redisOpen()) {
            String key = challengeKey(nonce);
            Object raw = redisUtils.get(key);
            redisUtils.delete(key);
            if (raw == null) {
                return null;
            }
            String json = raw instanceof String ? (String) raw : GsonConfig.getGson().toJson(raw);
            return GsonConfig.getGson().fromJson(json, Challenge.class);
        }
        return localChallenges.remove(nonce);
    }

    private void putSession(OpsSession session) {
        if (redisOpen()) {
            redisUtils.set(sessionKey(session.getSessionId()),
                    GsonConfig.getGson().toJson(session), SESSION_TTL_SEC);
        } else {
            localSessions.put(session.getSessionId(), session);
        }
    }

    private OpsSession getSession(String sessionId) {
        if (redisOpen()) {
            Object raw = redisUtils.get(sessionKey(sessionId));
            if (raw == null) {
                return null;
            }
            String json = raw instanceof String ? (String) raw : GsonConfig.getGson().toJson(raw);
            return GsonConfig.getGson().fromJson(json, OpsSession.class);
        }
        return localSessions.get(sessionId);
    }

    private void removeSession(String sessionId) {
        if (redisOpen()) {
            redisUtils.delete(sessionKey(sessionId));
        } else {
            localSessions.remove(sessionId);
        }
    }

    private static String stripPem(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
    }

    @Getter
    @Setter
    private static final class Challenge {
        private String certId;
        private long expireAt;

        Challenge() {
        }

        Challenge(String certId, long expireAt) {
            this.certId = certId;
            this.expireAt = expireAt;
        }
    }

    @Getter
    @Setter
    public static final class OpsSession {
        private String sessionId;
        private String certId;
        private String aesKeyBase64;
        private String aesIvBase64;
        private long expireAt;

        public OpsSession() {
        }

        public OpsSession(String sessionId, String certId, String aesKeyBase64, String aesIvBase64, long expireAt) {
            this.sessionId = sessionId;
            this.certId = certId;
            this.aesKeyBase64 = aesKeyBase64;
            this.aesIvBase64 = aesIvBase64;
            this.expireAt = expireAt;
        }
    }
}
