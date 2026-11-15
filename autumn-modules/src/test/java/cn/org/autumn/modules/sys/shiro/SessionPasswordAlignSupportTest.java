package cn.org.autumn.modules.sys.shiro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionPasswordAlignSupportTest {

    @Test
    void matchWhenEqual() {
        assertEquals(SessionPasswordAlignSupport.Result.MATCH,
                SessionPasswordAlignSupport.classify("abc", "abc"));
        assertEquals(SessionPasswordAlignSupport.Result.MATCH,
                SessionPasswordAlignSupport.classify(null, null));
    }

    @Test
    void syncWhenStorePbkdf2AndSessionLegacy() {
        String pbkdf2 = "pbkdf2$120000$c2FsdA==$aGFzaA==";
        String legacy = "6e4c6c96deadbeef";
        assertEquals(SessionPasswordAlignSupport.Result.SYNC_UPGRADE,
                SessionPasswordAlignSupport.classify(legacy, pbkdf2));
    }

    @Test
    void mismatchWhenBothPbkdf2Differ() {
        String a = "pbkdf2$120000$aaa$bbb";
        String b = "pbkdf2$120000$ccc$ddd";
        assertEquals(SessionPasswordAlignSupport.Result.MISMATCH,
                SessionPasswordAlignSupport.classify(a, b));
    }

    @Test
    void mismatchWhenSessionPbkdf2StoreLegacy() {
        String pbkdf2 = "pbkdf2$120000$c2FsdA==$aGFzaA==";
        String legacy = "6e4c6c96deadbeef";
        assertEquals(SessionPasswordAlignSupport.Result.MISMATCH,
                SessionPasswordAlignSupport.classify(pbkdf2, legacy));
    }

    @Test
    void mismatchWhenStoreBlank() {
        assertEquals(SessionPasswordAlignSupport.Result.MISMATCH,
                SessionPasswordAlignSupport.classify("legacy", null));
        assertEquals(SessionPasswordAlignSupport.Result.MISMATCH,
                SessionPasswordAlignSupport.classify("legacy", ""));
    }
}
