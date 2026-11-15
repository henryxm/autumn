package cn.org.autumn.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigListsTest {

    @Test
    void splitListSupportsCommaAndNewlines() {
        assertEquals(List.of("a.com", "b.com", "c.com"),
                SecurityConfigLists.splitList("a.com, b.com\nc.com;"));
    }

    @Test
    void normalizeSameSite() {
        List<String> fixes = new ArrayList<>();
        assertEquals("LAX", SecurityConfigLists.normalizeSameSite("lax", "LAX", fixes, "x"));
        assertEquals("LAX", SecurityConfigLists.normalizeSameSite("bad", "LAX", fixes, "x"));
        assertFalse(fixes.isEmpty());
    }

    @Test
    void authAndProxyValidate() {
        SecurityAuthConfig auth = new SecurityAuthConfig();
        auth.setCorsAllowedOrigins(null);
        auth.validateAndFix();
        assertEquals("", auth.getCorsAllowedOrigins());

        SecurityHttpProxyConfig proxy = new SecurityHttpProxyConfig();
        proxy.setAllowedHosts("api.openai.com,*.aliyuncs.com");
        assertEquals(2, proxy.hostList().size());

        SecuritySessionConfig session = new SecuritySessionConfig();
        session.setRememberMeSameSite("strict");
        session.validateAndFix();
        assertEquals("STRICT", session.getRememberMeSameSite());
    }
}
