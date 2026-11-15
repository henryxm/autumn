package cn.org.autumn.modules.sys.shiro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHashSupportTest {

    @Test
    void pbkdf2RoundTrip() {
        String stored = PasswordHashSupport.hashNew("Secret#123");
        assertTrue(PasswordHashSupport.isPbkdf2(stored));
        assertTrue(PasswordHashSupport.matches("Secret#123", stored, null));
        assertFalse(PasswordHashSupport.matches("wrong", stored, null));
    }

    @Test
    void legacySha256StillMatches() {
        String salt = "abcdefghijklmnopqrst";
        String legacy = ShiroUtils.sha256("admin", salt);
        assertFalse(PasswordHashSupport.isPbkdf2(legacy));
        assertTrue(PasswordHashSupport.matches("admin", legacy, salt));
    }
}
