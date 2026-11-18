package cn.org.autumn.modules.ops.service;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.OpsClientCertEntry;
import cn.org.autumn.model.RsaKey;
import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RsaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 运维证书：共享配置 OpsEnroll、状态机、重置轮换。
 */
class OpsCertServiceTest {

    private OpsCertService service;
    private SysConfigService sysConfigService;
    private final AtomicReference<SecurityOpsCertConfig> store = new AtomicReference<>(new SecurityOpsCertConfig());

    @BeforeEach
    void setUp() {
        service = new OpsCertService();
        sysConfigService = mock(SysConfigService.class);
        ReflectionTestUtils.setField(service, "sysConfigService", sysConfigService);
        when(sysConfigService.getSecurityOpsCertConfig()).thenAnswer(inv -> {
            SecurityOpsCertConfig c = store.get();
            return c == null ? new SecurityOpsCertConfig() : copy(c);
        });
        when(sysConfigService.saveSecurityOpsCertConfig(any())).thenAnswer(inv -> {
            SecurityOpsCertConfig c = inv.getArgument(0);
            store.set(copy(c));
            return c;
        });
    }

    @Test
    void init_issuesEnrollOnly_noServerRsa() {
        service.init();
        assertTrue(service.getConfig().hasEnrollMaterial());
        assertFalse(service.getConfig().hasServerKeyPair());
        assertEquals("pending", service.opsCertStatus());
    }

    @Test
    void ensureServerKeyPair_generatesOnce() {
        SecurityOpsCertConfig first = service.ensureServerKeyPair();
        assertTrue(first.hasServerKeyPair());
        assertNotNull(first.getFingerprint());
        String fp = first.getFingerprint();
        SecurityOpsCertConfig second = service.ensureServerKeyPair();
        assertEquals(fp, second.getFingerprint());
    }

    @Test
    void enroll_createsServerRsaAfterSecretAccepted() {
        byte[] salt = "ops-enroll-salt!".getBytes(StandardCharsets.UTF_8);
        String secret = "ops-one-time-enroll-secret";
        SecurityOpsCertConfig seeded = new SecurityOpsCertConfig();
        seeded.setEnrollSaltB64(Base64.getEncoder().encodeToString(salt));
        seeded.setEnrollHashB64(Base64.getEncoder().encodeToString(sha256(salt, secret)));
        seeded.setEnrollSecretConsumed(false);
        store.set(seeded);
        assertFalse(service.getConfig().hasServerKeyPair());

        RsaKey client = RsaUtil.generate(2048);
        OpsClientCertEntry entry = service.enroll(secret, client.getPublicKey(), "device-a");
        assertNotNull(entry.getCertId());
        assertTrue(service.getConfig().hasServerKeyPair());
        assertFalse(service.hasEnrollHash());
        assertEquals("ready", service.opsCertStatus());

        assertThrows(AException.class, () -> service.enroll(secret, client.getPublicKey(), "again"));

        SecurityOpsCertConfig cfg = service.getConfig();
        assertTrue(cfg.isEnrollSecretConsumed());
        assertFalse(cfg.hasEnrollMaterial());
    }

    @Test
    void enroll_wrongSecret_doesNotCreateServerRsa() {
        byte[] salt = "ops-enroll-salt!".getBytes(StandardCharsets.UTF_8);
        String secret = "correct-ops-enroll";
        SecurityOpsCertConfig seeded = new SecurityOpsCertConfig();
        seeded.setEnrollSaltB64(Base64.getEncoder().encodeToString(salt));
        seeded.setEnrollHashB64(Base64.getEncoder().encodeToString(sha256(salt, secret)));
        store.set(seeded);

        RsaKey client = RsaUtil.generate(2048);
        assertThrows(AException.class, () -> service.enroll("wrong", client.getPublicKey(), "x"));
        assertTrue(service.hasEnrollHash());
        assertFalse(service.getConfig().hasServerKeyPair());
    }

    @Test
    void ensureEnroll_writesSharedMaterial_notOnlyMemory() {
        service.ensureEnrollSecretIfNeeded();
        assertTrue(service.getConfig().hasEnrollMaterial());
        assertTrue(service.isEnrollAvailable());
        assertFalse(service.getConfig().hasServerKeyPair());
        String salt = service.getConfig().getEnrollSaltB64();
        service.ensureEnrollSecretIfNeeded();
        assertEquals(salt, service.getConfig().getEnrollSaltB64());
    }

    @Test
    void reset_clearsClients_andReissuesEnroll_withoutServerRsa() {
        service.ensureServerKeyPair();
        SecurityOpsCertConfig seeded = service.getConfig();
        OpsClientCertEntry c = new OpsClientCertEntry();
        c.setCertId("ops-old");
        c.setClientPublicKey("pubkey");
        c.setRevoked(false);
        seeded.setClients(new ArrayList<>());
        seeded.getClients().add(c);
        seeded.setEnrollSecretConsumed(true);
        sysConfigService.saveSecurityOpsCertConfig(seeded);

        SecurityOpsCertConfig after = service.reset();
        assertTrue(after.getClients().isEmpty());
        assertFalse(after.isEnrollSecretConsumed());
        assertTrue(service.hasEnrollHash());
        assertFalse(after.hasServerKeyPair());
        assertEquals("pending", service.opsCertStatus());
    }

    @Test
    void session_open_withSignatureAndAesWrap() {
        byte[] salt = "ops-enroll-salt!".getBytes(StandardCharsets.UTF_8);
        String secret = "ops-session-enroll";
        SecurityOpsCertConfig seeded = new SecurityOpsCertConfig();
        seeded.setEnrollSaltB64(Base64.getEncoder().encodeToString(salt));
        seeded.setEnrollHashB64(Base64.getEncoder().encodeToString(sha256(salt, secret)));
        store.set(seeded);
        RsaKey client = RsaUtil.generate(2048);
        OpsClientCertEntry entry = service.enroll(secret, client.getPublicKey(), "sess");
        assertTrue(service.getConfig().hasServerKeyPair());

        OpsSessionService sessions = new OpsSessionService();
        ReflectionTestUtils.setField(sessions, "opsCertService", service);
        ReflectionTestUtils.setField(sessions, "sysConfigService", sysConfigService);
        when(sysConfigService.getNameSpace()).thenReturn("test");
        String nonce = sessions.createChallenge(entry.getCertId());
        String signature = OpsSessionService.sign(client.getPrivateKey(), nonce);
        String aesKeyB64 = Base64.getEncoder().encodeToString("0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        String wrapped = RsaUtil.encrypt(aesKeyB64, service.getConfig().getServerPublicKey());
        OpsSessionService.OpsSession session = sessions.openSession(
                entry.getCertId(), nonce, signature, wrapped, "");
        assertNotNull(session.getSessionId());
        assertEquals(entry.getCertId(), session.getCertId());
        assertNotNull(sessions.requireSession(session.getSessionId()));
    }

    private static SecurityOpsCertConfig copy(SecurityOpsCertConfig src) {
        SecurityOpsCertConfig c = new SecurityOpsCertConfig();
        c.setSiteId(src.getSiteId());
        c.setServerPublicKey(src.getServerPublicKey());
        c.setServerPrivateKey(src.getServerPrivateKey());
        c.setFingerprint(src.getFingerprint());
        c.setEnrollSecretConsumed(src.isEnrollSecretConsumed());
        c.setEnrollSaltB64(src.getEnrollSaltB64());
        c.setEnrollHashB64(src.getEnrollHashB64());
        c.setResetAt(src.getResetAt());
        c.setClients(src.getClients() == null ? new ArrayList<>() : new ArrayList<>(src.getClients()));
        return c;
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
}
