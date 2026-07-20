package cn.org.autumn.node;

import cn.org.autumn.config.Config;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * {@code node-profile.json} 磁盘读写（进程内锁 + 原子写）。
 * <p>
 * {@code home} 默认每次按 {@link #HOME_KEY} 从 {@link Config} 解析（避免构造期早于 Spring Environment）；
 * 仅 {@link #home(String, boolean)} 显式切换后固定覆盖路径。
 */
@Slf4j
@Component
public class ProfileStore {

    public static final String FILE_NAME = "node-profile.json";
    public static final String DEFAULT_HOME = ".autumn";
    public static final String HOME_KEY = "autumn.node.home";

    private final ReentrantLock lock = new ReentrantLock();
    /** 非 null 表示运维/API 显式指定目录，不再跟配置漂移。 */
    private volatile Path homeOverride;

    public Path home() {
        Path override = homeOverride;
        if (override != null) {
            return override;
        }
        return resolveHome(Config.getEnv(HOME_KEY));
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
            String json = JSON.toJSONString(profile, JSONWriter.Feature.PrettyFormat);
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

    static Path resolveHome(String raw) {
        String s = StringUtils.trimToNull(raw);
        if (s == null) {
            s = DEFAULT_HOME;
        }
        Path p = Path.of(s);
        if (!p.isAbsolute()) {
            p = Path.of(System.getProperty("user.dir", ".")).resolve(p).normalize();
        }
        return p.toAbsolutePath().normalize();
    }
}
