package cn.org.autumn.model;

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
}
