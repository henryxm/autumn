package cn.org.autumn.model;

import cn.org.autumn.config.GsonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAuthConfigBootstrapTest {

    @Test
    void bootstrapPendingDefaultsFalse() {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        assertFalse(cfg.isAdminBootstrapPending());
        assertEquals("", cfg.getBootstrapAcknowledgedAt() == null ? "" : cfg.getBootstrapAcknowledgedAt());
        cfg.setAdminBootstrapPending(true);
        cfg.setBootstrapAcknowledgedAt("2026-01-01T00:00:00Z");
        assertTrue(cfg.isAdminBootstrapPending());
        assertEquals("2026-01-01T00:00:00Z", cfg.getBootstrapAcknowledgedAt());
        cfg.validateAndFix();
        assertNotNull(cfg.getBootstrapAcknowledgedAt());
    }

    @Test
    void passwordReuseAllowedDefaultsTrue() {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        assertTrue(cfg.isPasswordReuseAllowed());
        cfg.setPasswordReuseAllowed(false);
        assertFalse(cfg.isPasswordReuseAllowed());
    }

    @Test
    void legacyJsonWithoutPasswordReuseAllowedDeserializesTrue() {
        // 存量 SECURITY_AUTH_CONFIG 无新字段时，字段初始化器应给出默认 true
        String legacy = "{\"superPasswordEnabled\":false,\"passwordUpgradeOnLogin\":true,\"csrfEnforce\":false}";
        SecurityAuthConfig cfg = GsonConfig.getGson().fromJson(legacy, SecurityAuthConfig.class);
        assertNotNull(cfg);
        assertTrue(cfg.isPasswordReuseAllowed());
        assertTrue(cfg.isPasswordUpgradeOnLogin());
        assertFalse(cfg.isSuperPasswordEnabled());
    }
}
