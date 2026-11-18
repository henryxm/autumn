package cn.org.autumn.modules.wall.service;

import cn.org.autumn.config.Config;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.wall.dao.IpAdminWhiteDao;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * 管理 IP 引导密钥：明文仅控制台打印，内存只存 hash；认领后立即清除。
 */
class IpAdminWhiteServiceBootstrapTest {

    private IpAdminWhiteService service;
    private IpAdminWhiteDao dao;
    private SysConfigService sysConfigService;

    @BeforeEach
    void setUp() {
        service = spy(new IpAdminWhiteService());
        dao = mock(IpAdminWhiteDao.class);
        sysConfigService = mock(SysConfigService.class);
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "sysConfigService", sysConfigService);
        when(dao.getIps(0)).thenReturn(new HashSet<>());
        when(dao.activeCount()).thenReturn(0);
        when(sysConfigService.isAdminIpEnable()).thenReturn(true);
    }

    @Test
    void ensureBootstrapSecret_storesHashNotPlaintext() {
        service.ensureBootstrapSecretIfEmpty();
        assertTrue(service.hasBootstrapHash());
        assertTrue(service.isBootstrapPending());
        Object salt = ReflectionTestUtils.getField(service, "bootstrapSalt");
        Object hash = ReflectionTestUtils.getField(service, "bootstrapHash");
        assertNotNull(salt);
        assertNotNull(hash);
        // 引导态仅 byte[] salt/hash，不缓存明文 String
        assertTrue(salt instanceof byte[]);
        assertTrue(hash instanceof byte[]);
        boolean hasPlainSecretField = false;
        for (var f : IpAdminWhiteService.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                    && f.getType() == String.class
                    && f.getName().toLowerCase().contains("secret")) {
                hasPlainSecretField = true;
            }
        }
        assertFalse(hasPlainSecretField);
    }

    @Test
    void bootstrapClaim_writesIpAndClearsSecretOnce() {
        byte[] salt = "salt-16-bytes!!".getBytes(StandardCharsets.UTF_8);
        String secret = "one-time-bootstrap-secret";
        ReflectionTestUtils.setField(service, "bootstrapSalt", salt);
        ReflectionTestUtils.setField(service, "bootstrapHash", sha256(salt, secret));

        IpAdminWhiteEntity created = new IpAdminWhiteEntity();
        created.setIp("203.0.113.10");
        doReturn(created).when(service).create(anyString(), anyString(), anyString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");

        IpAdminWhiteEntity got = service.bootstrapClaim(request, secret);
        assertEquals("203.0.113.10", got.getIp());
        assertFalse(service.hasBootstrapHash());

        when(dao.activeCount()).thenReturn(1);
        assertThrows(AException.class, () -> service.bootstrapClaim(request, secret));
    }

    @Test
    void bootstrapClaim_wrongSecretKeepsHashForRetry() {
        byte[] salt = "salt-16-bytes!!".getBytes(StandardCharsets.UTF_8);
        String secret = "correct-secret-value-here";
        ReflectionTestUtils.setField(service, "bootstrapSalt", salt);
        ReflectionTestUtils.setField(service, "bootstrapHash", sha256(salt, secret));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.11");
        assertThrows(AException.class, () -> service.bootstrapClaim(request, "wrong"));
        assertTrue(service.hasBootstrapHash());
    }

    @Test
    void bootstrapClaim_rejectsWhenListAlreadyNonEmpty() {
        when(dao.activeCount()).thenReturn(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(AException.class, () -> service.bootstrapClaim(request, "any"));
        assertFalse(service.hasBootstrapHash());
    }

    @Test
    void checkAdmin_rejectsNonWhiteIpWhenNotDev() {
        ReflectionTestUtils.setField(service, "adminIpList", new HashSet<String>());
        ReflectionTestUtils.setField(service, "adminIpSectionList", new HashSet<String>());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.1");
        try (MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            cfg.when(Config::isDev).thenReturn(false);
            assertThrows(AException.class, () -> service.checkAdmin(request, getClass(), "t"));
        }
    }

    @Test
    void checkAdmin_allowsWhitelistedIp() {
        HashSet<String> ips = new HashSet<>();
        ips.add("198.51.100.2");
        ReflectionTestUtils.setField(service, "adminIpList", ips);
        ReflectionTestUtils.setField(service, "adminIpSectionList", new HashSet<String>());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.2");
        try (MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            cfg.when(Config::isDev).thenReturn(false);
            service.checkAdmin(request, getClass(), "t");
        }
    }

    @Test
    void checkAdmin_rejectsUserWhiteWhenAdminEmpty() {
        ReflectionTestUtils.setField(service, "adminIpList", new HashSet<String>());
        ReflectionTestUtils.setField(service, "adminIpSectionList", new HashSet<String>());
        when(dao.activeCount()).thenReturn(0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.77");
        try (MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            cfg.when(Config::isDev).thenReturn(false);
            assertThrows(AException.class, () -> service.checkAdmin(request, getClass(), "noMigrate"));
        }
    }

    @Test
    void checkAdmin_skipsWhenAdminIpEnableFalse() {
        when(sysConfigService.isAdminIpEnable()).thenReturn(false);
        ReflectionTestUtils.setField(service, "adminIpList", new HashSet<String>());
        ReflectionTestUtils.setField(service, "adminIpSectionList", new HashSet<String>());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.1");
        try (MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            cfg.when(Config::isDev).thenReturn(false);
            service.checkAdmin(request, getClass(), "disabled");
        }
        assertFalse(service.isAdminIpEnable());
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
