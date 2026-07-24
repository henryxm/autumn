package cn.org.autumn.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson2.JSON;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileServiceTest {

    @TempDir
    Path temp;

    @AfterEach
    void clearTtlProp() {
        System.clearProperty(ProfileService.CACHE_TTL_KEY);
    }

    @Test
    void ensure_createsStableUuidAndEmptyRoles() {
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("node").toString(), false);
        ProfileService svc = new ProfileService(store);
        Profile a = svc.ensure();
        assertTrue(Uuid.isValid(a.getUuid()));
        assertFalse(a.adjusted());
        assertTrue(a.getRoles().isEmpty());
        Profile b = svc.ensure();
        assertEquals(a.getUuid(), b.getUuid());
        assertEquals(a.getUuid(), svc.uuid());
    }

    @Test
    void patch_roles_marksAdjusted_uuidImmutable() {
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("node2").toString(), false);
        ProfileService svc = new ProfileService(store);
        String id = svc.ensure().getUuid();
        svc.roles("job", "api");
        assertTrue(svc.adjusted());
        assertTrue(svc.has("job"));
        assertEquals(id, svc.uuid());
        assertThrows(IllegalArgumentException.class, () -> svc.patch(p -> p.setUuid(Uuid.uuid())));
    }

    @Test
    void home_migrate_preservesUuid() {
        ProfileStore store = new ProfileStore();
        Path first = temp.resolve("a");
        Path second = temp.resolve("b");
        store.home(first.toString(), false);
        ProfileService svc = new ProfileService(store);
        String id = svc.ensure().getUuid();
        svc.home(second.toString(), true);
        assertEquals(id, svc.uuid());
        assertTrue(svc.file().toString().contains(second.getFileName().toString()));
    }

    @Test
    void manualFileEdit_takesEffectAfterCacheTtl() throws Exception {
        System.setProperty(ProfileService.CACHE_TTL_KEY, "30");
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("edit").toString(), false);
        ProfileService svc = new ProfileService(store);
        Profile p = svc.ensure();
        assertFalse(svc.adjusted());
        p.setRoles(List.of("job"));
        Files.writeString(store.file(), JSON.toJSONString(p), StandardCharsets.UTF_8);
        assertFalse(svc.adjusted());
        Thread.sleep(50L);
        assertTrue(svc.adjusted());
        assertTrue(svc.has("job"));
        assertEquals(p.getUuid(), svc.uuid());
    }

    @Test
    void reload_bypassesCacheTtl() throws Exception {
        System.setProperty(ProfileService.CACHE_TTL_KEY, "60000");
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("reload").toString(), false);
        ProfileService svc = new ProfileService(store);
        Profile p = svc.ensure();
        p.setRoles(List.of("api"));
        Files.writeString(store.file(), JSON.toJSONString(p), StandardCharsets.UTF_8);
        assertFalse(svc.adjusted());
        svc.reload();
        assertTrue(svc.has("api"));
    }

    @Test
    void peekUuid_doesNotEnsure() {
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("peek").toString(), false);
        ProfileService svc = new ProfileService(store);
        assertEquals(null, svc.peekUuid());
        svc.ensure();
        assertTrue(Uuid.isValid(svc.peekUuid()));
    }

    @Test
    void customizer_onlyOnCreate() {
        AtomicInteger creates = new AtomicInteger();
        ProfileCustomizer c = (profile, snap) -> {
            creates.incrementAndGet();
            Map<String, String> m = new LinkedHashMap<>(profile.getLabels());
            m.put("k", "v");
            profile.setLabels(m);
        };
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("cust").toString(), false);
        ProfileService svc = new ProfileService(store, List.of(c));
        Profile created = svc.ensure();
        assertEquals(1, creates.get());
        assertEquals("v", created.getLabels().get("k"));
        svc.ensure();
        assertEquals(1, creates.get());
    }

    @Test
    void customizer_onLoad_backfillsAndPersists() {
        AtomicInteger loads = new AtomicInteger();
        ProfileCustomizer c = new ProfileCustomizer() {
            @Override
            public void onCreate(Profile profile, Fingerprint.Snapshot snap) {
            }

            @Override
            public boolean onLoad(Profile profile, Fingerprint.Snapshot snap) {
                loads.incrementAndGet();
                if (profile.getLabels().containsKey("k")) {
                    return false;
                }
                Map<String, String> m = new LinkedHashMap<>(profile.getLabels());
                m.put("k", "v");
                profile.setLabels(m);
                return true;
            }
        };
        ProfileStore store = new ProfileStore();
        store.home(temp.resolve("load").toString(), false);
        ProfileService first = new ProfileService(store);
        first.ensure();
        assertEquals(0, loads.get());
        ProfileService second = new ProfileService(store, List.of(c));
        Profile p = second.ensure();
        assertEquals(1, loads.get());
        assertEquals("v", p.getLabels().get("k"));
        ProfileService third = new ProfileService(store, List.of(c));
        assertEquals("v", third.ensure().getLabels().get("k"));
        assertEquals(2, loads.get());
    }
}
