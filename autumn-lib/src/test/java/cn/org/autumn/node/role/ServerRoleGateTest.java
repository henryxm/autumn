package cn.org.autumn.node.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerRoleGateTest {

    private ServerRoleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ServerRoleRegistry();
        new BuiltinServerRoles(registry).must();
    }

    @Test
    void normalize_allExclusive() {
        assertEquals(List.of("ALL"), ServerRoleGroups.normalize(List.of("WEB", "ALL", "API")));
        assertEquals(List.of("WEB", "API"), ServerRoleGroups.normalize(List.of("web", " api ", "WEB")));
        assertTrue(ServerRoleGroups.normalize(List.of()).isEmpty());
    }

    @Test
    void isUnrestricted_emptyOrAll() {
        assertTrue(ServerRoleGate.isUnrestricted(List.of()));
        assertTrue(ServerRoleGate.isUnrestricted(List.of("ALL")));
        assertFalse(ServerRoleGate.isUnrestricted(List.of("API")));
    }

    @Test
    void hasCapability_viaBuiltin() {
        assertTrue(ServerRoleGate.hasCapability(List.of(), ServerRole.CAP_WEB_UI));
        assertTrue(ServerRoleGate.hasCapability(List.of("ALL"), ServerRole.CAP_API_HTTP));
        assertTrue(ServerRoleGate.hasCapability(List.of("API"), ServerRole.CAP_API_HTTP));
        assertTrue(ServerRoleGate.hasCapability(List.of("API"), ServerRole.CAP_FILE_DOWNLOAD));
        assertFalse(ServerRoleGate.hasCapability(List.of("API"), ServerRole.CAP_WEB_UI));
        assertTrue(ServerRoleGate.hasCapability(List.of("WEB", "API"), ServerRole.CAP_WEB_UI));
    }

    @Test
    void allowsAll_compatible() {
        assertTrue(ServerRoleGate.allowsAll());
        assertTrue(ServerRoleGate.allowsAll((String[]) null));
        assertTrue(ServerRoleGate.allowsAll(""));
    }

    @Test
    void builtinRegistered() {
        assertTrue(registry.contains("ALL"));
        assertTrue(registry.contains("WEB"));
        assertFalse(registry.contains("AGENT"));
        ServerRole api = registry.get("API");
        assertTrue(api.hasCapability(ServerRole.CAP_API_HTTP));
        assertFalse(api.hasCapability(ServerRole.CAP_WEB_UI));
    }
}
