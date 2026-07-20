package cn.org.autumn.node;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistryFreshnessTest {

    @Test
    void isFreshEntry_requiresBeatWithinStaleWindow() {
        long now = 1_000_000L;
        assertFalse(Registry.isFreshEntry(null, now, Registry.STALE_MS));
        assertFalse(Registry.isFreshEntry("{}", now, Registry.STALE_MS));
        assertFalse(Registry.isFreshEntry("{\"uuid\":\"x\"}", now, Registry.STALE_MS));
        assertTrue(Registry.isFreshEntry("{\"beat\":" + now + "}", now, Registry.STALE_MS));
        assertTrue(Registry.isFreshEntry("{\"beat\":" + (now - 1000) + "}", now, Registry.STALE_MS));
        assertFalse(Registry.isFreshEntry("{\"beat\":" + (now - Registry.STALE_MS - 1) + "}", now, Registry.STALE_MS));
    }
}
