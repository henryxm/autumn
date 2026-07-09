package cn.org.autumn.modules.sys.shiro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class HostSessionCookieSupportTest {

    @Test
    void hasMultipleIndependentDomains_detectsUnrelatedTlds() {
        assertTrue(HostSessionCookieSupport.hasMultipleIndependentDomains(Arrays.asList("a.com", "b.com")));
        assertFalse(HostSessionCookieSupport.hasMultipleIndependentDomains(Collections.singletonList("a.com")));
        assertFalse(HostSessionCookieSupport.hasMultipleIndependentDomains(Arrays.asList("auth.x.com", "app.x.com")));
    }

    @Test
    void resolveCookieDomain_usesClusterRootForSubdomain() {
        assertEquals(".x.com", HostSessionCookieSupport.resolveCookieDomain("auth.x.com", "x.com"));
        assertNull(HostSessionCookieSupport.resolveCookieDomain("a.com", "x.com"));
    }

    @Test
    void resolveCookieName_multiIndependentUsesDefault() {
        assertEquals(HostSessionCookieSupport.DEFAULT_COOKIE_NAME,
                HostSessionCookieSupport.resolveCookieName("a.com", "x.com", true));
    }

    @Test
    void parseDomainList_skipsCommentedEntries() {
        assertEquals(Arrays.asList("a.com", "b.com"),
                HostSessionCookieSupport.parseDomainList("a.com,#skip.com,b.com"));
    }

    @Test
    void resolveHolder_multiIndependentHostOnly() {
        HostSessionCookieHolder holder = HostSessionCookieSupport.resolveHolder(
                "b.com", Arrays.asList("a.com", "b.com"), "x.com");
        assertEquals(HostSessionCookieSupport.DEFAULT_COOKIE_NAME, holder.getName());
        assertNull(holder.getDomain());
    }

    @Test
    void resolveHolder_subdomainUsesClusterRoot() {
        HostSessionCookieHolder holder = HostSessionCookieSupport.resolveHolder(
                "auth.x.com", Arrays.asList("auth.x.com"), "x.com");
        assertEquals("x.com", holder.getName());
        assertEquals(".x.com", holder.getDomain());
    }
}
