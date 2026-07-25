package cn.org.autumn.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RegistryFreshnessTest {

    @AfterEach
    void clearProps() {
        System.clearProperty(Registry.REGISTRY_KEY);
        System.clearProperty(Registry.REDIS_OPEN_KEY);
    }

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

    @Test
    void parseMember_extractsProfileAndSelf() {
        long now = 2_000_000L;
        String json = "{\"uuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"beat\":" + now
                + ",\"profile\":{\"uuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"version\":1,"
                + "\"roles\":[\"LOCAL\",\"WEB\"],\"labels\":{\"zone\":\"edge\"}}}";
        Map<String, Object> row = Registry.parseMember("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", json, now,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", row.get("uuid"));
        assertTrue((Boolean) row.get("online"));
        assertTrue((Boolean) row.get("self"));
        assertEquals(List.of("LOCAL", "WEB"), row.get("roles"));
        @SuppressWarnings("unchecked")
        Map<String, String> labels = (Map<String, String>) row.get("labels");
        assertEquals("edge", labels.get("zone"));
    }

    @Test
    void enabledConfig_followsRedisWhenRegistryUnset() {
        System.clearProperty(Registry.REGISTRY_KEY);
        System.setProperty(Registry.REDIS_OPEN_KEY, "false");
        assertFalse(Registry.isEnabledConfig());
        System.setProperty(Registry.REDIS_OPEN_KEY, "true");
        assertTrue(Registry.isEnabledConfig());
    }

    @Test
    void enabledConfig_explicitRegistryOverridesRedis() {
        System.setProperty(Registry.REDIS_OPEN_KEY, "true");
        System.setProperty(Registry.REGISTRY_KEY, "false");
        assertFalse(Registry.isEnabledConfig());
        System.setProperty(Registry.REGISTRY_KEY, "true");
        System.setProperty(Registry.REDIS_OPEN_KEY, "false");
        assertTrue(Registry.isEnabledConfig());
    }
}
