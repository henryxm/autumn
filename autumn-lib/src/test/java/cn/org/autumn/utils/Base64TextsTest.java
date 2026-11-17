package cn.org.autumn.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base64TextsTest {

    @Test
    void normalize_restoresPlusAfterFormUrlDecode() {
        String original = "YWFh+YmJi/Y2Nj==";
        String mangled = original.replace('+', ' ');
        assertEquals(original, Base64Texts.normalize(mangled));
    }

    @Test
    void normalize_stripsCrLfTab() {
        assertEquals("YWJj", Base64Texts.normalize("YW\r\nJ\tc"));
    }

    @Test
    void decode_roundTrip_afterFormPlusToSpace() {
        byte[] raw = "hello+world/test".getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.getEncoder().encodeToString(raw);
        String mangled = b64.replace('+', ' ');
        assertArrayEquals(raw, Base64Texts.decode(mangled));
    }

    @Test
    void decode_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> Base64Texts.decode("   "));
    }
}
