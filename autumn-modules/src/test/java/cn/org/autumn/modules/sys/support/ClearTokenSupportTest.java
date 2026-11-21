package cn.org.autumn.modules.sys.support;

import cn.org.autumn.exception.AException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearTokenSupportTest {

    @AfterEach
    void reset() {
        ClearTokenSupport.resetForTests();
    }

    @Test
    void validateNewToken_rejectsShort() {
        assertThrows(AException.class, () -> ClearTokenSupport.validateNewToken("short"));
        ClearTokenSupport.validateNewToken("sixteen-chars-ok");
    }

    @Test
    void matches_constantTimeAndLocks() {
        assertTrue(ClearTokenSupport.matches("secret-token-here", "secret-token-here", "k1"));
        assertFalse(ClearTokenSupport.matches("secret-token-here", "wrong", "k2"));
        for (int i = 0; i < 8; i++) {
            assertFalse(ClearTokenSupport.matches("secret-token-here", "wrong-" + i, "lock-me"));
        }
        assertThrows(AException.class, () ->
                ClearTokenSupport.matches("secret-token-here", "wrong-last", "lock-me"));
    }
}
