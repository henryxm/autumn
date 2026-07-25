package cn.org.autumn.node.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.org.autumn.job.JobDuty;
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
        assertTrue(ServerRoleGate.hasCapability(List.of("LOCAL"), ServerRole.CAP_LOCAL_JOB));
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
        assertTrue(registry.contains("LOCAL"));
        assertFalse(registry.contains("AGENT"));
        ServerRole api = registry.get("API");
        assertTrue(api.hasCapability(ServerRole.CAP_API_HTTP));
        assertFalse(api.hasCapability(ServerRole.CAP_WEB_UI));
        ServerRole local = registry.get("LOCAL");
        assertTrue(local.hasCapability(ServerRole.CAP_LOCAL_JOB));
        assertFalse(local.hasCapability(ServerRole.CAP_SCHEDULED_JOB));
    }

    @Test
    void isLocalScoped_matrix() {
        assertFalse(ServerRoleGate.isLocalScoped(List.of()));
        assertFalse(ServerRoleGate.isLocalScoped(List.of("ALL")));
        assertFalse(ServerRoleGate.isLocalScoped(List.of("JOB")));
        assertFalse(ServerRoleGate.isLocalScoped(List.of("WEB", "API")));
        assertTrue(ServerRoleGate.isLocalScoped(List.of("LOCAL")));
        assertTrue(ServerRoleGate.isLocalScoped(List.of("LOCAL", "WEB", "API")));
        assertFalse(ServerRoleGate.isLocalScoped(List.of("LOCAL", "JOB")));
    }

    @Test
    void allowsClusterJobDuty_loose() {
        List<String> empty = List.of();
        List<String> localOnly = List.of("LOCAL", "WEB");
        List<String> localAndJob = List.of("LOCAL", "JOB");
        List<String> jobOnly = List.of("JOB");

        assertTrue(ServerRoleGate.allowsClusterJobDuty(empty, JobDuty.SINGLETON));
        assertTrue(ServerRoleGate.allowsClusterJobDuty(empty, JobDuty.ALL));
        assertTrue(ServerRoleGate.allowsClusterJobDuty(localOnly, JobDuty.ALL));
        assertTrue(ServerRoleGate.allowsClusterJobDuty(localOnly, JobDuty.LOCAL));
        assertFalse(ServerRoleGate.allowsClusterJobDuty(localOnly, JobDuty.SINGLETON));
        assertFalse(ServerRoleGate.allowsClusterJobDuty(localOnly, JobDuty.SEQUENTIAL));
        assertTrue(ServerRoleGate.allowsClusterJobDuty(localAndJob, JobDuty.SINGLETON));
        assertTrue(ServerRoleGate.allowsClusterJobDuty(jobOnly, JobDuty.SEQUENTIAL));
    }
}
