package cn.org.autumn.modules.sys.shiro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AdminBootstrapPasswordSupportTest {

    @TempDir
    Path temp;

    @Test
    void generateHasExpectedLength() {
        String a = AdminBootstrapPasswordSupport.generate();
        String b = AdminBootstrapPasswordSupport.generate();
        assertEquals(20, a.length());
        assertEquals(20, b.length());
        assertNotEquals(a, b);
    }

    @Test
    void writeReadDeleteUnderNodeHome() {
        Path home = temp.resolve("node-home");
        Path file = AdminBootstrapPasswordSupport.writeCredentials(home, "admin", "PlainPass!23456789012");
        assertTrue(Files.isRegularFile(file));
        assertTrue(file.toString().replace('\\', '/').endsWith("security/admin-bootstrap.credentials"));
        assertEquals("PlainPass!23456789012", AdminBootstrapPasswordSupport.readPassword(home));
        assertTrue(AdminBootstrapPasswordSupport.exists(home));

        AdminBootstrapPasswordSupport.deleteQuietly(home);
        assertFalse(AdminBootstrapPasswordSupport.exists(home));
        assertNull(AdminBootstrapPasswordSupport.readPassword(home));
    }

    @Test
    void blankPasswordDoesNotCreateUsableFile() {
        Path home = temp.resolve("empty-home");
        AdminBootstrapPasswordSupport.writeCredentials(home, "admin", "   ");
        // blank after trim → write skipped content-wise; file may still not exist or empty
        assertNull(AdminBootstrapPasswordSupport.readPassword(home));
    }

    @Test
    void resolveFileUsesRelativeUnderHome() {
        Path home = temp.resolve("h");
        Path f = AdminBootstrapPasswordSupport.resolveFile(home);
        assertEquals(home.toAbsolutePath().normalize().resolve(AdminBootstrapPasswordSupport.RELATIVE_UNDER_HOME),
                f);
    }
}
