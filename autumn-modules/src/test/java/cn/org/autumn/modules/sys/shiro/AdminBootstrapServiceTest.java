package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.model.SecurityAuthConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.node.ProfileStore;
import org.apache.shiro.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminBootstrapServiceTest {

    @TempDir
    Path temp;

    @Mock
    SysConfigService sysConfigService;

    @Mock
    EnvBean envBean;

    @Mock
    ProfileStore profileStore;

    @Mock
    Session session;

    @InjectMocks
    AdminBootstrapService service;

    @BeforeEach
    void home() {
        lenient().when(profileStore.home()).thenReturn(temp.resolve("node"));
    }

    @Test
    void reminderNoneWhenNotPending() {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        cfg.setAdminBootstrapPending(false);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(cfg);

        Map<String, Object> r = service.buildPasswordReminder(null);
        assertEquals(false, r.get("pending"));
        assertEquals("none", r.get("level"));
    }

    @Test
    void reminderStrongWhenPending() {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        cfg.setAdminBootstrapPending(true);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(cfg);

        Map<String, Object> r = service.buildPasswordReminder(null);
        assertEquals(true, r.get("pending"));
        assertEquals("strong", r.get("level"));
    }

    @Test
    void reminderWeakWhenSnoozed() {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        cfg.setAdminBootstrapPending(true);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(cfg);
        when(session.getAttribute(AdminBootstrapService.SESSION_SNOOZE_UNTIL))
                .thenReturn(System.currentTimeMillis() + 60_000);

        Map<String, Object> r = service.buildPasswordReminder(session);
        assertEquals("weak", r.get("level"));
        assertEquals(true, r.get("snoozed"));
    }

    @Test
    void systemPasswordSkipsBootstrapFile() {
        when(envBean.getSystemPassword()).thenReturn("ConfiguredPassword12345");
        service.onBootstrapAdminCreated("admin", "should-not-write");
        assertFalse(AdminBootstrapPasswordSupport.exists(temp.resolve("node")));
        verify(sysConfigService, never()).saveSecurityAuthConfig(any());
    }

    @Test
    void bootstrapCreatesFileAndMarksPending() {
        when(envBean.getSystemPassword()).thenReturn("");
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(new SecurityAuthConfig());

        service.onBootstrapAdminCreated("admin", "BootstrapSecret!234567");
        assertTrue(AdminBootstrapPasswordSupport.exists(temp.resolve("node")));
        assertEquals("BootstrapSecret!234567",
                AdminBootstrapPasswordSupport.readPassword(temp.resolve("node")));
        verify(sysConfigService).saveSecurityAuthConfig(argThat(c -> c.isAdminBootstrapPending()));
    }

    @Test
    void clearDeletesFileAndClearsPending() {
        Path home = temp.resolve("node");
        AdminBootstrapPasswordSupport.writeCredentials(home, "admin", "x".repeat(20));
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        cfg.setAdminBootstrapPending(true);
        when(sysConfigService.getSecurityAuthConfig()).thenReturn(cfg);

        service.clearBootstrapState("password_changed");
        assertFalse(AdminBootstrapPasswordSupport.exists(home));
        verify(sysConfigService).saveSecurityAuthConfig(argThat(c -> !c.isAdminBootstrapPending()));
    }
}
