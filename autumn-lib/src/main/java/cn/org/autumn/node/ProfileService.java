package cn.org.autumn.node;

import cn.org.autumn.config.Config;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.utils.Uuid;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * 本机节点画像服务：读/写 {@code node-profile.json}，提供稳定 {@link #uuid()} 供任意项目用作节点主键。
 *
 * <h3>加载与刷新（手动改文件如何生效）</h3>
 * <ol>
 *   <li><b>启动</b>：{@link LoadFactory.Must#must()} → {@link #ensure()} 读盘或指纹创建；
 *       首次创建调 {@link ProfileCustomizer#onCreate}，已有文件调 {@link ProfileCustomizer#onLoad}（可补 labels 并回写）。</li>
 *   <li><b>API 写路径</b>：{@link #save}/{@link #patch}/{@link #roles} 等写盘后刷新内存缓存与 TTL。</li>
 *   <li><b>缓存 TTL</b>：默认 1 分钟（{@code autumn.node.profile.cache-ttl-ms}）。到期后读路径丢弃缓存，按需再读盘。</li>
 *   <li><b>显式</b>：HTTP {@code POST /sys/node/profile/reload} 或 {@link #reload()}。</li>
 * </ol>
 * <p>
 * 已有文件路径仅在 {@link #lastSnapshot()} 为空时采集指纹（避免 TTL/二次 ensure 重复扫硬件）。
 * 启动仅保证身份（{@code roles} 为空）不算手动调整；非空 {@code roles} 才参与 LoopJob 角色门禁。
 * {@link #peekUuid()} 只读缓存、不触发 ensure；业务身份请在 Must/Init 之后用 {@link #uuid()}。
 */
@Slf4j
@Service
public class ProfileService implements LoadFactory.Must, NodeProfile {

    public static final String CACHE_TTL_KEY = "autumn.node.profile.cache-ttl-ms";
    public static final long DEFAULT_CACHE_TTL_MS = 60_000L;

    private final ProfileStore store;
    private final ObjectProvider<FingerprintProvider> fingerprintProvider;
    private final ObjectProvider<ProfileCustomizer> customizers;

    private volatile Profile cached;
    private volatile long cachedAtMs = 0L;
    /** 最近一次采集快照（ensure 创建或显式 collect）；供扩展层读取。 */
    private volatile Fingerprint.Snapshot lastSnapshot;

    public ProfileService(ProfileStore store,
                          ObjectProvider<FingerprintProvider> fingerprintProvider,
                          ObjectProvider<ProfileCustomizer> customizers) {
        this.store = store;
        this.fingerprintProvider = fingerprintProvider;
        this.customizers = customizers;
    }

    /** 测试便捷构造。 */
    public ProfileService(ProfileStore store) {
        this(store, emptyFingerprintProvider(), emptyCustomizerProvider());
    }

    public ProfileService(ProfileStore store, List<ProfileCustomizer> customizerList) {
        this(store, emptyFingerprintProvider(), providerOf(customizerList));
    }

    @Override
    @Order(10)
    public void must() {
        ensure();
    }

    @Override
    public Profile profile() {
        return get();
    }

    @Override
    public Profile get() {
        expireCacheIfNeeded();
        Profile p = cached;
        if (p == null) {
            ensure();
            p = cached;
        }
        return p == null ? null : p.copy();
    }

    @Override
    public String uuid() {
        Profile p = get();
        return p != null ? p.getUuid() : null;
    }

    /** 仅读内存缓存；未 ensure 过则返回 null，不落盘、不采指纹。 */
    @Override
    public String peekUuid() {
        Profile p = cached;
        return p != null ? p.getUuid() : null;
    }

    @Override
    public List<String> roles() {
        Profile p = get();
        return p == null ? List.of() : new ArrayList<>(p.getRoles());
    }

    @Override
    public boolean has(String role) {
        if (StringUtils.isBlank(role)) {
            return false;
        }
        String want = role.trim();
        for (String r : roles()) {
            if (want.equalsIgnoreCase(StringUtils.trimToEmpty(r))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAll(String... required) {
        if (required == null || required.length == 0) {
            return true;
        }
        for (String r : required) {
            if (StringUtils.isNotBlank(r) && !has(r)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasAll(List<String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        return hasAll(required.toArray(new String[0]));
    }

    @Override
    public boolean adjusted() {
        Profile p = get();
        return p != null && p.adjusted();
    }

    @Override
    public Map<String, String> labels() {
        Profile p = get();
        return p == null ? Map.of() : new LinkedHashMap<>(p.getLabels());
    }

    @Override
    public Path home() {
        return store.home();
    }

    @Override
    public Path file() {
        return store.file();
    }

    public long cachedAtMs() {
        return cachedAtMs;
    }

    public long cacheTtlMs() {
        return resolveCacheTtlMs();
    }

    /** 最近一次指纹快照（ensure 后通常非 null）。 */
    @Override
    public Fingerprint.Snapshot lastSnapshot() {
        return lastSnapshot;
    }

    /**
     * 无文件则指纹生成并落盘；有则加载到内存。
     * 首次创建调用 {@link ProfileCustomizer#onCreate}；已有文件调用 {@link ProfileCustomizer#onLoad}（可补 labels 并回写）。
     * 已有文件仅在 {@code lastSnapshot == null} 时采集指纹，避免重复扫硬件。
     */
    @Override
    public synchronized Profile ensure() {
        Profile existing = store.read();
        if (existing != null && Uuid.isValid(existing.getUuid())) {
            normalizeCollections(existing);
            Fingerprint.Snapshot snap = lastSnapshot;
            if (snap == null) {
                snap = collectSnapshot();
                lastSnapshot = snap;
            }
            if (invokeLoadCustomizers(existing, snap)) {
                store.write(existing);
            }
            adopt(existing);
            return existing.copy();
        }
        Fingerprint.Snapshot snap = collectSnapshot();
        lastSnapshot = snap;
        Profile created = new Profile();
        created.setUuid(StringUtils.isNotBlank(snap.hash32()) && Uuid.isValid(snap.hash32()) ? snap.hash32() : Uuid.uuid());
        created.setVersion(1);
        created.setRoles(new ArrayList<>());
        created.setLabels(new LinkedHashMap<>());
        invokeCreateCustomizers(created, snap);
        store.write(created);
        adopt(created);
        if (log.isInfoEnabled()) {
            log.info("Node profile ensured uuid={} file={}", created.getUuid(), store.file());
        }
        return created.copy();
    }

    public synchronized void save(Profile profile) {
        Objects.requireNonNull(profile, "profile");
        if (!Uuid.isValid(profile.getUuid())) {
            throw new IllegalArgumentException("illegal uuid: " + profile.getUuid());
        }
        Profile current = cached != null ? cached : store.read();
        if (current != null && Uuid.isValid(current.getUuid())
                && !Uuid.equals(current.getUuid(), profile.getUuid())) {
            throw new IllegalArgumentException("uuid immutable; use resetUuid()");
        }
        if (StringUtils.isBlank(profile.getCreate()) && current != null) {
            profile.setCreate(current.getCreate());
        }
        store.write(profile);
        adopt(profile.copy());
    }

    @Override
    public synchronized Profile patch(Consumer<Profile> mutator) {
        Objects.requireNonNull(mutator, "mutator");
        Profile p = ensure();
        Profile work = p.copy();
        String uuidBefore = work.getUuid();
        mutator.accept(work);
        if (!Uuid.equals(uuidBefore, work.getUuid())) {
            throw new IllegalArgumentException("uuid immutable in patch; use resetUuid()");
        }
        store.write(work);
        adopt(work);
        return work.copy();
    }

    @Override
    public synchronized Profile patch(Map<String, Object> fields) {
        return patch(p -> applyPatchMap(p, fields));
    }

    @Override
    public synchronized Profile roles(List<String> roles) {
        return patch(p -> p.setRoles(roles != null ? new ArrayList<>(roles) : new ArrayList<>()));
    }

    @Override
    public synchronized Profile roles(String... roles) {
        return roles(roles == null ? List.of() : Arrays.asList(roles));
    }

    @Override
    public synchronized Profile label(String key, String value) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("label key blank");
        }
        return patch(p -> {
            Map<String, String> m = new LinkedHashMap<>(p.getLabels());
            if (value == null) {
                m.remove(key.trim());
            } else {
                m.put(key.trim(), value);
            }
            p.setLabels(m);
        });
    }

    @Override
    public synchronized Profile labels(Map<String, String> labels) {
        return patch(p -> p.setLabels(labels != null ? new LinkedHashMap<>(labels) : new LinkedHashMap<>()));
    }

    public synchronized Profile home(String dir) {
        return home(dir, false);
    }

    public synchronized Profile home(String dir, boolean migrate) {
        store.home(dir, migrate);
        dropCache();
        return ensure();
    }

    @Override
    public synchronized Profile reload() {
        Profile p = store.read();
        if (p == null || !Uuid.isValid(p.getUuid())) {
            dropCache();
            return ensure();
        }
        normalizeCollections(p);
        adopt(p);
        if (log.isDebugEnabled()) {
            log.debug("Node profile reload uuid={} file={}", p.getUuid(), store.file());
        }
        return p.copy();
    }

    public synchronized Profile resetUuid() {
        Profile p = ensure();
        Fingerprint.Snapshot snap = collectSnapshot();
        lastSnapshot = snap;
        Profile work = p.copy();
        work.setUuid(StringUtils.isNotBlank(snap.hash32()) && Uuid.isValid(snap.hash32()) ? snap.hash32() : Uuid.uuid());
        store.write(work);
        adopt(work);
        log.warn("Node profile uuid reset to {}", work.getUuid());
        return work.copy();
    }

    private Fingerprint.Snapshot collectSnapshot() {
        FingerprintProvider provider = fingerprintProvider.getIfAvailable();
        if (provider != null) {
            Fingerprint.Snapshot snap = provider.collect();
            if (snap != null) {
                return snap;
            }
        }
        return Fingerprint.collect();
    }

    private void invokeCreateCustomizers(Profile profile, Fingerprint.Snapshot snap) {
        List<ProfileCustomizer> list = orderedCustomizers();
        for (ProfileCustomizer c : list) {
            try {
                c.onCreate(profile, snap);
            } catch (Exception e) {
                log.warn("ProfileCustomizer onCreate failed: {}", e.toString());
            }
        }
    }

    /** @return 是否有 Customizer 改动了 profile（需落盘） */
    private boolean invokeLoadCustomizers(Profile profile, Fingerprint.Snapshot snap) {
        boolean dirty = false;
        List<ProfileCustomizer> list = orderedCustomizers();
        for (ProfileCustomizer c : list) {
            try {
                if (c.onLoad(profile, snap)) {
                    dirty = true;
                }
            } catch (Exception e) {
                log.warn("ProfileCustomizer onLoad failed: {}", e.toString());
            }
        }
        return dirty;
    }

    private List<ProfileCustomizer> orderedCustomizers() {
        List<ProfileCustomizer> list = new ArrayList<>();
        customizers.forEach(list::add);
        list.sort(AnnotationAwareOrderComparator.INSTANCE);
        return list;
    }

    private synchronized void expireCacheIfNeeded() {
        Profile p = cached;
        if (p == null) {
            return;
        }
        long ttl = resolveCacheTtlMs();
        if (ttl <= 0L) {
            dropCache();
            return;
        }
        long age = System.currentTimeMillis() - cachedAtMs;
        if (age >= ttl) {
            dropCache();
            if (log.isDebugEnabled()) {
                log.debug("Node profile cache expired ageMs={} ttlMs={}", age, ttl);
            }
        }
    }

    private void adopt(Profile p) {
        cached = p;
        cachedAtMs = System.currentTimeMillis();
    }

    private void dropCache() {
        cached = null;
        cachedAtMs = 0L;
    }

    private static long resolveCacheTtlMs() {
        String raw = Config.getEnv(CACHE_TTL_KEY);
        if (StringUtils.isBlank(raw)) {
            return DEFAULT_CACHE_TTL_MS;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_CACHE_TTL_MS;
        }
    }

    private static void normalizeCollections(Profile p) {
        p.setRoles(p.getRoles() == null ? new ArrayList<>() : new ArrayList<>(p.getRoles()));
        p.setLabels(p.getLabels() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(p.getLabels()));
    }

    private static void applyPatchMap(Profile p, Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        if (fields.containsKey("uuid")) {
            Object u = fields.get("uuid");
            if (u != null && StringUtils.isNotBlank(String.valueOf(u))
                    && !Uuid.equals(p.getUuid(), String.valueOf(u))) {
                throw new IllegalArgumentException("uuid immutable in patch; use resetUuid()");
            }
        }
        if (fields.containsKey("version") && fields.get("version") instanceof Number n) {
            p.setVersion(n.intValue());
        }
        if (fields.containsKey("roles")) {
            Object r = fields.get("roles");
            if (r instanceof List<?> list) {
                List<String> roles = new ArrayList<>();
                for (Object o : list) {
                    if (o != null) {
                        roles.add(String.valueOf(o));
                    }
                }
                p.setRoles(roles);
            }
        }
        if (fields.containsKey("labels") && fields.get("labels") instanceof Map<?, ?> map) {
            Map<String, String> labels = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    labels.put(String.valueOf(e.getKey()), e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
            p.setLabels(labels);
        }
    }

    private static ObjectProvider<FingerprintProvider> emptyFingerprintProvider() {
        return new ObjectProvider<>() {
            @Override
            public FingerprintProvider getObject() {
                return null;
            }

            @Override
            public FingerprintProvider getIfAvailable() {
                return null;
            }

            @Override
            public FingerprintProvider getIfUnique() {
                return null;
            }
        };
    }

    private static ObjectProvider<ProfileCustomizer> emptyCustomizerProvider() {
        return providerOf(List.of());
    }

    private static ObjectProvider<ProfileCustomizer> providerOf(List<ProfileCustomizer> list) {
        List<ProfileCustomizer> copy = list == null ? List.of() : List.copyOf(list);
        return new ObjectProvider<>() {
            @Override
            public ProfileCustomizer getObject() {
                return copy.isEmpty() ? null : copy.get(0);
            }

            @Override
            public ProfileCustomizer getIfAvailable() {
                return getObject();
            }

            @Override
            public ProfileCustomizer getIfUnique() {
                return copy.size() == 1 ? copy.get(0) : null;
            }

            @Override
            public void forEach(Consumer<? super ProfileCustomizer> action) {
                copy.forEach(action);
            }
        };
    }
}
