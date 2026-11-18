package cn.org.autumn.modules.ops.service;

import cn.org.autumn.config.GsonConfig;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.OpsPendingClaimEntry;
import cn.org.autumn.model.RsaKey;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.RsaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 运维证书：服务端密钥对、一次性 OpsEnroll、客户端公钥注册。
 * 与业务 {@code /rsa/api/v1} 及管理 IP bootstrap 密钥分离。
 * <p>
 * 单机/集群同一真源：{@link SecurityOpsCertConfig}（含 OpsEnroll salt/hash，无明文）。
 */
@Slf4j
@Service
public class OpsCertService implements InitFactory.Init {

    public static final String ENROLL_LOG_PREFIX = "OpsEnroll secret=";

    /** 认领码有效期（毫秒）。 */
    public static final long CLAIM_TTL_MS = 30L * 60L * 1000L;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public void init() {
        // Boot only issues OpsEnroll; server RSA is created on successful enroll (when secret is used).
        ensureEnrollSecretIfNeeded();
    }

    public SecurityOpsCertConfig getConfig() {
        return sysConfigService.getSecurityOpsCertConfig();
    }

    /**
     * 空则写：已有密钥对则跳过，避免集群各节点各生成一份。
     */
    public synchronized SecurityOpsCertConfig ensureServerKeyPair() {
        SecurityOpsCertConfig existing = getConfig();
        if (existing.hasServerKeyPair()) {
            return existing;
        }
        RsaKey pair = RsaUtil.generate(2048);
        SecurityOpsCertConfig cfg = copy(existing);
        cfg.setServerPublicKey(pair.getPublicKey());
        cfg.setServerPrivateKey(pair.getPrivateKey());
        cfg.setFingerprint(fingerprint(pair.getPublicKey()));
        if (StringUtils.isBlank(cfg.getSiteId())) {
            cfg.setSiteId("autumn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        // 竞态：他节点可能先写入，以 DB 为准
        SecurityOpsCertConfig after = getConfig();
        if (after.hasServerKeyPair() && !cfg.getFingerprint().equals(after.getFingerprint())) {
            log.info("OpsCert server RSA using existing cluster key fingerprint={}", after.getFingerprint());
            return after;
        }
        log.info("OpsCert server RSA key pair generated fingerprint={}", after.getFingerprint());
        return after;
    }

    /**
     * 若共享配置尚无 enroll 材料且需要入驻，生成并写入 salt/hash；明文仅本机日志一次。
     */
    public synchronized void ensureEnrollSecretIfNeeded() {
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (cfg.isEnrollSecretConsumed() && hasActiveClient()) {
            if (cfg.hasEnrollMaterial()) {
                cfg.setEnrollSaltB64("");
                cfg.setEnrollHashB64("");
                sysConfigService.saveSecurityOpsCertConfig(cfg);
            }
            return;
        }
        if (cfg.hasEnrollMaterial()) {
            return;
        }
        if (cfg.isEnrollSecretConsumed() && !hasActiveClient()) {
            cfg.setEnrollSecretConsumed(false);
        }
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        byte[] plain = new byte[32];
        secureRandom.nextBytes(plain);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(plain);
        byte[] hash = sha256(salt, secret);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        cfg.setEnrollSaltB64(saltB64);
        cfg.setEnrollHashB64(hashB64);
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        SecurityOpsCertConfig after = getConfig();
        // 仅当本节点写入获胜时打印明文，避免集群多份不同 secret
        if (saltB64.equals(after.getEnrollSaltB64()) && hashB64.equals(after.getEnrollHashB64())) {
            log.warn("{}{}", ENROLL_LOG_PREFIX, secret);
        } else {
            log.info("OpsCert enroll material written by another node; skip printing OpsEnroll on this node");
        }
    }

    public boolean isEnrollAvailable() {
        SecurityOpsCertConfig cfg = getConfig();
        if (cfg.hasEnrollMaterial()) {
            return true;
        }
        return !cfg.isEnrollSecretConsumed() || !hasActiveClient();
    }

    public boolean hasActiveClient() {
        SecurityOpsCertConfig cfg = getConfig();
        if (cfg.getClients() == null) {
            return false;
        }
        for (OpsClientCertEntry c : cfg.getClients()) {
            if (c != null && !c.isRevoked() && StringUtils.isNotBlank(c.getClientPublicKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return opsCert 状态：none / pending / ready（不触发生成服务端 RSA）
     */
    public String opsCertStatus() {
        SecurityOpsCertConfig cfg = getConfig();
        if (hasActiveClient()) {
            return "ready";
        }
        if (isEnrollAvailable() || !cfg.isEnrollSecretConsumed()) {
            return "pending";
        }
        return "none";
    }

    /**
     * 校验 OpsEnroll secret 成功后再生成服务端 RSA（若尚无），并登记客户端公钥。
     */
    public synchronized OpsClientCertEntry enroll(String secret, String clientPublicKey, String label) {
        if (StringUtils.isBlank(clientPublicKey)) {
            throw new AException("客户端公钥不能为空", 400);
        }
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (!cfg.hasEnrollMaterial()) {
            throw new AException("入驻密钥不可用", 403);
        }
        if (StringUtils.isBlank(secret)) {
            throw new AException("入驻密钥无效", 403);
        }
        byte[] salt;
        byte[] expect;
        try {
            salt = Base64.getDecoder().decode(cfg.getEnrollSaltB64());
            expect = Base64.getDecoder().decode(cfg.getEnrollHashB64());
        } catch (Exception e) {
            throw new AException("入驻密钥不可用", 403);
        }
        byte[] got = sha256(salt, secret.trim());
        if (!MessageDigest.isEqual(got, expect)) {
            log.warn("SecurityAlert denied reason=ops_enroll detail=invalid_enroll_secret");
            throw new AException("入驻密钥无效", 403);
        }
        // Secret accepted → create server RSA now (not at boot / probe).
        ensureServerKeyPair();
        cfg = copy(getConfig());
        OpsClientCertEntry entry = new OpsClientCertEntry();
        entry.setCertId("ops-" + UUID.randomUUID().toString().replace("-", ""));
        entry.setClientPublicKey(normalizeKey(clientPublicKey));
        entry.setLabel(StringUtils.defaultIfBlank(label, "ops-client"));
        entry.setCreatedAt(System.currentTimeMillis());
        entry.setLastUsedAt(entry.getCreatedAt());
        entry.setRevoked(false);
        if (cfg.getClients() == null) {
            cfg.setClients(new ArrayList<>());
        }
        cfg.getClients().add(entry);
        cfg.setEnrollSecretConsumed(true);
        cfg.setEnrollSaltB64("");
        cfg.setEnrollHashB64("");
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        log.info("OpsCert client enrolled certId={} label={} fingerprint={}", entry.getCertId(), entry.getLabel(), cfg.getFingerprint());
        return entry;
    }

    public OpsClientCertEntry findActive(String certId) {
        if (StringUtils.isBlank(certId)) {
            return null;
        }
        SecurityOpsCertConfig cfg = getConfig();
        if (cfg.getClients() == null) {
            return null;
        }
        for (OpsClientCertEntry c : cfg.getClients()) {
            if (c != null && certId.equals(c.getCertId()) && !c.isRevoked()) {
                return c;
            }
        }
        return null;
    }

    public synchronized void touch(String certId) {
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (cfg.getClients() == null) {
            return;
        }
        for (OpsClientCertEntry c : cfg.getClients()) {
            if (c != null && certId.equals(c.getCertId())) {
                c.setLastUsedAt(System.currentTimeMillis());
                sysConfigService.saveSecurityOpsCertConfig(cfg);
                return;
            }
        }
    }

    /**
     * @return map: success, lastCert（吊销后是否已无未吊销客户端）
     */
    public synchronized Map<String, Object> revoke(String certId) {
        Map<String, Object> out = new HashMap<>();
        out.put("success", false);
        out.put("lastCert", false);
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (cfg.getClients() == null) {
            return out;
        }
        boolean found = false;
        for (OpsClientCertEntry c : cfg.getClients()) {
            if (c != null && certId.equals(c.getCertId())) {
                c.setRevoked(true);
                found = true;
                break;
            }
        }
        if (!found) {
            return out;
        }
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        boolean last = !hasActiveClient();
        out.put("success", true);
        out.put("lastCert", last);
        if (last) {
            log.warn("SecurityAlert ops_cert_last_revoked certId={} no active clients remain", certId);
        }
        return out;
    }

    public int countActiveClients() {
        SecurityOpsCertConfig cfg = getConfig();
        if (cfg.getClients() == null) {
            return 0;
        }
        int n = 0;
        for (OpsClientCertEntry c : cfg.getClients()) {
            if (c != null && !c.isRevoked() && StringUtils.isNotBlank(c.getClientPublicKey())) {
                n++;
            }
        }
        return n;
    }

    /**
     * 已登录设备签发一次性认领码；明文 secret 仅返回一次。
     *
     * @return claimId, claimSecret, expiresAt, siteId, serverFingerprint, label
     */
    public synchronized Map<String, Object> issueClaim(String issuedByCertId, String label) {
        if (!hasActiveClient() || !getConfig().hasServerKeyPair()) {
            throw new AException("站点尚未完成运维证书入驻", 403);
        }
        if (StringUtils.isBlank(issuedByCertId) || findActive(issuedByCertId) == null) {
            throw new AException("签发方证书无效", 403);
        }
        purgeExpiredClaims();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        byte[] plain = new byte[24];
        secureRandom.nextBytes(plain);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(plain);
        String claimId = "claim-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + CLAIM_TTL_MS;
        OpsPendingClaimEntry claim = new OpsPendingClaimEntry();
        claim.setClaimId(claimId);
        claim.setSaltB64(Base64.getEncoder().encodeToString(salt));
        claim.setSecretHashB64(Base64.getEncoder().encodeToString(sha256(salt, secret)));
        claim.setLabel(StringUtils.defaultIfBlank(label, "ops-device"));
        claim.setIssuedByCertId(issuedByCertId);
        claim.setExpiresAt(expiresAt);
        claim.setConsumed(false);
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (cfg.getPendingClaims() == null) {
            cfg.setPendingClaims(new ArrayList<>());
        }
        cfg.getPendingClaims().add(claim);
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        log.info("OpsCert claim issued claimId={} by={} expiresAt={}", claimId, issuedByCertId, expiresAt);
        Map<String, Object> data = new HashMap<>();
        data.put("claimId", claimId);
        data.put("claimSecret", secret);
        data.put("expiresAt", expiresAt);
        data.put("siteId", cfg.getSiteId());
        data.put("serverFingerprint", cfg.getFingerprint());
        data.put("label", claim.getLabel());
        return data;
    }

    /**
     * 新设备用认领码提交公钥，登记独立 certId（私钥仅新设备本地）。
     */
    public synchronized OpsClientCertEntry claim(String claimSecret, String clientPublicKey, String label) {
        if (StringUtils.isBlank(clientPublicKey)) {
            throw new AException("客户端公钥不能为空", 400);
        }
        if (StringUtils.isBlank(claimSecret)) {
            throw new AException("认领码无效", 403);
        }
        if (!getConfig().hasServerKeyPair()) {
            throw new AException("站点尚未完成运维证书入驻", 403);
        }
        purgeExpiredClaims();
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (cfg.getPendingClaims() == null || cfg.getPendingClaims().isEmpty()) {
            throw new AException("认领码无效或已过期", 403);
        }
        String secret = claimSecret.trim();
        OpsPendingClaimEntry matched = null;
        for (OpsPendingClaimEntry p : cfg.getPendingClaims()) {
            if (p == null || p.isConsumed() || p.getExpiresAt() < System.currentTimeMillis()) {
                continue;
            }
            byte[] salt;
            byte[] expect;
            try {
                salt = Base64.getDecoder().decode(p.getSaltB64());
                expect = Base64.getDecoder().decode(p.getSecretHashB64());
            } catch (Exception e) {
                continue;
            }
            if (MessageDigest.isEqual(sha256(salt, secret), expect)) {
                matched = p;
                break;
            }
        }
        if (matched == null) {
            log.warn("SecurityAlert denied reason=ops_claim detail=invalid_or_expired_claim");
            throw new AException("认领码无效或已过期", 403);
        }
        matched.setConsumed(true);
        OpsClientCertEntry entry = new OpsClientCertEntry();
        entry.setCertId("ops-" + UUID.randomUUID().toString().replace("-", ""));
        entry.setClientPublicKey(normalizeKey(clientPublicKey));
        String resolvedLabel = StringUtils.isNotBlank(label) ? label.trim()
                : StringUtils.defaultIfBlank(matched.getLabel(), "ops-device");
        entry.setLabel(resolvedLabel);
        entry.setCreatedAt(System.currentTimeMillis());
        entry.setLastUsedAt(entry.getCreatedAt());
        entry.setRevoked(false);
        if (cfg.getClients() == null) {
            cfg.setClients(new ArrayList<>());
        }
        cfg.getClients().add(entry);
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        log.info("OpsCert client claimed certId={} label={} claimId={}",
                entry.getCertId(), entry.getLabel(), matched.getClaimId());
        return entry;
    }

    public List<OpsClientCertEntry> listClients() {
        SecurityOpsCertConfig cfg = getConfig();
        return cfg.getClients() == null ? List.of() : new ArrayList<>(cfg.getClients());
    }

    /**
     * 重置：清空客户端与服务端钥、重新发放 OpsEnroll；服务端 RSA 仍延后到下次 enroll。
     */
    public synchronized SecurityOpsCertConfig reset() {
        SecurityOpsCertConfig cfg = new SecurityOpsCertConfig();
        cfg.setEnrollSecretConsumed(false);
        cfg.setEnrollSaltB64("");
        cfg.setEnrollHashB64("");
        cfg.setResetAt(System.currentTimeMillis());
        cfg.setClients(new ArrayList<>());
        cfg.setPendingClaims(new ArrayList<>());
        sysConfigService.saveSecurityOpsCertConfig(cfg);
        ensureEnrollSecretIfNeeded();
        log.warn("OpsCert reset completed; OpsEnroll reissued (server RSA deferred until enroll)");
        return getConfig();
    }

    private synchronized void purgeExpiredClaims() {
        SecurityOpsCertConfig cfg = copy(getConfig());
        if (cfg.getPendingClaims() == null || cfg.getPendingClaims().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<OpsPendingClaimEntry> it = cfg.getPendingClaims().iterator();
        while (it.hasNext()) {
            OpsPendingClaimEntry p = it.next();
            if (p == null || p.isConsumed() || p.getExpiresAt() < now) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            sysConfigService.saveSecurityOpsCertConfig(cfg);
        }
    }

    public boolean hasEnrollHash() {
        return getConfig().hasEnrollMaterial();
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private static String fingerprint(String publicKey) {
        try {
            byte[] raw = Base64.getDecoder().decode(normalizeKey(publicKey));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(8, dig.length); i++) {
                sb.append(String.format("%02x", dig[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] sha256(byte[] salt, String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(secret.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static SecurityOpsCertConfig copy(SecurityOpsCertConfig src) {
        if (src == null) {
            return new SecurityOpsCertConfig();
        }
        return GsonConfig.getGson().fromJson(GsonConfig.getGson().toJson(src), SecurityOpsCertConfig.class);
    }
}
