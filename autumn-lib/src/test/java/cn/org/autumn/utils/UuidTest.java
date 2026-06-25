package cn.org.autumn.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UuidTest {

    @Test
    void norm_stripsHyphensForStandardUuid() {
        String norm = Uuid.norm("A1B2C3D4-E5F6-4789-90AA-BBCCDDEEFF00");
        assertEquals("a1b2c3d4e5f6478990aabbccddeeff00", norm);
        assertEquals(Uuid.LENGTH, norm.length());
    }

    @Test
    void norm_hashesLongCompositeIds() {
        String composite = "user-uuid:key-uuid:jsonconv:body-conv-id";
        String norm = Uuid.norm(composite);
        assertEquals(Uuid.LENGTH, norm.length());
        assertEquals(norm, Uuid.norm(composite));
        assertFalse(Uuid.isValid(composite));
    }

    @Test
    void norm_hashesShortIdsTo32Hex() {
        String norm = Uuid.norm("ltu-ABC123");
        assertEquals(Uuid.LENGTH, norm.length());
        assertNotNull(norm);
        assertFalse(Uuid.isValid("ltu-ABC123"));
    }
}
