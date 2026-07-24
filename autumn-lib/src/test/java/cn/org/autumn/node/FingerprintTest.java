package cn.org.autumn.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.org.autumn.utils.Uuid;
import java.util.List;
import org.junit.jupiter.api.Test;

class FingerprintTest {

    @Test
    void normalizeMaterial_macOrderIndependent() {
        String a = Fingerprint.normalizeMaterial(List.of("aa:bb", "11:22"), "mid-1", "host-a", "os", "arch");
        String b = Fingerprint.normalizeMaterial(List.of("11:22", "aa:bb"), "mid-1", "host-a", "os", "arch");
        assertEquals(a, b);
        assertEquals(Fingerprint.sha256Prefix32(a), Fingerprint.sha256Prefix32(b));
    }

    @Test
    void normalizeMaterial_hostnameChangesHash() {
        String a = Fingerprint.sha256Prefix32(
                Fingerprint.normalizeMaterial(List.of(), "", "host-a", "os", "arch"));
        String b = Fingerprint.sha256Prefix32(
                Fingerprint.normalizeMaterial(List.of(), "", "host-b", "os", "arch"));
        assertNotEquals(a, b);
    }

    @Test
    void collect_hash32IsValidUuid() {
        Fingerprint.Snapshot snap = Fingerprint.collect();
        assertTrue(Uuid.isValid(snap.hash32()));
        assertEquals(snap.hash32(), Fingerprint.generate());
    }
}
