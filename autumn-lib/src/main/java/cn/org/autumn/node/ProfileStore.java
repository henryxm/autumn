package cn.org.autumn.node;

import cn.org.autumn.config.Config;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * {@code node-profile.json} 磁盘读写（进程内锁 + 原子写）。
 * <p>
 * {@code home} 默认每次按 {@link #HOME_KEY} 从 {@link Config} 解析（避免构造期早于 Spring Environment）；
 * 仅 {@link #home(String, boolean)} 显式切换后固定覆盖路径。
 * <p>
 * <b>写盘策略</b>：只覆盖框架已定义字段（{@link #FRAMEWORK_KEYS}），磁盘上其它顶层键（子项目扩展）原样保留。
 */
@Slf4j
@Component
public class ProfileStore {

    public static final String FILE_NAME = "node-profile.json";
    public static final String DEFAULT_HOME = ".autumn";
    public static final String HOME_KEY = "autumn.node.home";

    /** 框架画像字段；写盘时仅更新这些键，其它顶层配置保留。 */
    public static final List<String> FRAMEWORK_KEYS = List.of(
            "uuid", "version", "create", "update", "roles", "labels");

    private final ReentrantLock lock = new ReentrantLock();
    private final ObjectProvider<Environment> environment;
    /** 非 null 表示运维/API 显式指定目录，不再跟配置漂移。 */
    private volatile Path homeOverride;

    @Autowired
    public ProfileStore(ObjectProvider<Environment> environment) {
        this.environment = environment;
    }

    /** 测试 / 无 Spring 场景。 */
    public ProfileStore() {
        this.environment = null;
    }

    public Path home() {
        Path override = homeOverride;
        if (override != null) {
            return override;
        }
        return resolveHome(resolveConfiguredHome());
    }

    /** 配置键 → 系统属性 → Spring Environment（含 relaxed / EPP 默认）。 */
    String resolveConfiguredHome() {
        String raw = Config.getEnv(HOME_KEY);
        if (StringUtils.isNotBlank(raw)) {
            return raw;
        }
        Environment env = environment != null ? environment.getIfAvailable() : null;
        if (env != null) {
            String p = env.getProperty(HOME_KEY);
            if (StringUtils.isNotBlank(p)) {
                return p;
            }
        }
        return null;
    }

    public Path file() {
        return home().resolve(FILE_NAME);
    }

    /**
     * 切换画像目录；{@code migrate=true} 时若旧文件存在且新文件不存在则复制。
     */
    public void home(String dir, boolean migrate) {
        lock.lock();
        try {
            Path oldHome = home();
            Path oldFile = oldHome.resolve(FILE_NAME);
            Path next = resolveHome(dir);
            if (migrate && Files.isRegularFile(oldFile) && !Files.isRegularFile(next.resolve(FILE_NAME))) {
                Files.createDirectories(next);
                Files.copy(oldFile, next.resolve(FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
            }
            this.homeOverride = next;
        } catch (IOException e) {
            throw new IllegalStateException("set home failed: " + dir, e);
        } finally {
            lock.unlock();
        }
    }

    public void home(String dir) {
        home(dir, false);
    }

    public Profile read() {
        lock.lock();
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) {
                return null;
            }
            String json = Files.readString(f, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(json)) {
                return null;
            }
            return JSON.parseObject(json, Profile.class);
        } catch (Exception e) {
            log.warn("read profile failed path={}: {}", file(), e.toString());
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 原子写盘：用 Profile 覆盖 {@link #FRAMEWORK_KEYS}，保留文件中其它顶层键（子项目扩展）。
     */
    public void write(Profile profile) {
        Objects.requireNonNull(profile, "profile");
        if (!Uuid.isValid(profile.getUuid())) {
            throw new IllegalArgumentException("illegal uuid: " + profile.getUuid());
        }
        lock.lock();
        try {
            Path dir = home();
            Files.createDirectories(dir);
            Path target = file();
            Path tmp = dir.resolve(FILE_NAME + ".tmp");
            String now = Instant.now().toString();
            if (StringUtils.isBlank(profile.getCreate())) {
                profile.setCreate(now);
            }
            profile.setUpdate(now);
            JSONObject out = readRawUnlocked(target);
            overlayFrameworkFields(out, profile);
            String json = JSON.toJSONString(out, JSONWriter.Feature.PrettyFormat);
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("write profile failed: " + file(), e);
        } finally {
            lock.unlock();
        }
    }

    /** 将框架字段叠到已有 JSON（原地修改 {@code out}）。 */
    static void overlayFrameworkFields(JSONObject out, Profile profile) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(profile, "profile");
        JSONObject framed = (JSONObject) JSON.toJSON(profile);
        if (framed == null) {
            return;
        }
        for (String key : FRAMEWORK_KEYS) {
            if (framed.containsKey(key)) {
                out.put(key, framed.get(key));
            }
        }
    }

    private static JSONObject readRawUnlocked(Path target) {
        if (!Files.isRegularFile(target)) {
            return new JSONObject();
        }
        try {
            String json = Files.readString(target, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(json)) {
                return new JSONObject();
            }
            JSONObject o = JSON.parseObject(json);
            return o != null ? o : new JSONObject();
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static Path resolveHome(String raw) {
        String s = StringUtils.trimToNull(raw);
        if (s == null) {
            // 相对默认目录落在 user.home，避免 Docker 下 user.dir=/ 写成 /.autumn
            return Path.of(System.getProperty("user.home", "."), DEFAULT_HOME).toAbsolutePath().normalize();
        }
        Path p = Path.of(s);
        if (!p.isAbsolute()) {
            p = Path.of(System.getProperty("user.home", ".")).resolve(p).normalize();
        }
        return p.toAbsolutePath().normalize();
    }
}
