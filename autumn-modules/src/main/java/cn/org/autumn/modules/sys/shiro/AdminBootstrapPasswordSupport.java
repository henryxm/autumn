package cn.org.autumn.modules.sys.shiro;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * 管理员引导口令：生成、落盘恢复文件、清理。
 * <p>
 * 文件默认 {@code {node.home}/security/admin-bootstrap.credentials}（节点独有，改密后删除）。
 * 不把明文写入 DB/SysConfig。
 */
@Slf4j
public final class AdminBootstrapPasswordSupport {

    public static final String RELATIVE_UNDER_HOME = "security/admin-bootstrap.credentials";

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private AdminBootstrapPasswordSupport() {
    }

    public static Path resolveFile(Path nodeHome) {
        Path home = nodeHome != null
                ? nodeHome.toAbsolutePath().normalize()
                : Path.of(System.getProperty("user.home", "."), ".autumn").toAbsolutePath().normalize();
        return home.resolve(RELATIVE_UNDER_HOME).toAbsolutePath().normalize();
    }

    public static String generate() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < 20; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * 写入恢复文件并打 warn（含路径与明文一次）。返回写入路径；失败仍返回期望路径。
     */
    public static Path writeCredentials(Path nodeHome, String username, String plainPassword) {
        Path file = resolveFile(nodeHome);
        if (StringUtils.isBlank(plainPassword)) {
            return file;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String body = "username=" + StringUtils.defaultString(username) + "\n"
                    + "password=" + plainPassword.trim() + "\n"
                    + "createdAt=" + Instant.now() + "\n"
                    + "# Change the password after first login; this file is removed after password change or acknowledge-retain\n";
            Files.writeString(file, body, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            tryRestrictPermissions(file);
            // Keep info: one-shot plaintext for operators when random bootstrap password is created
            log.info("Admin bootstrap password generated. Save it now. file={} username={} password={} Change it after login.",
                    file, username, plainPassword.trim());
        } catch (Exception e) {
            log.error("Failed to write admin bootstrap credentials file path={} err={} password (log only)={}",
                    file, e.getMessage(), plainPassword.trim());
        }
        return file;
    }

    public static String readPassword(Path nodeHome) {
        Path file = resolveFile(nodeHome);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line == null || line.startsWith("#")) {
                    continue;
                }
                String t = line.trim();
                if (t.startsWith("password=")) {
                    String v = t.substring("password=".length()).trim();
                    return StringUtils.isNotBlank(v) ? v : null;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read admin bootstrap credentials file path={} err={}", file, e.getMessage());
        }
        return null;
    }

    public static boolean exists(Path nodeHome) {
        return Files.isRegularFile(resolveFile(nodeHome));
    }

    public static void deleteQuietly(Path nodeHome) {
        Path file = resolveFile(nodeHome);
        try {
            if (Files.isRegularFile(file)) {
                Files.delete(file);
                if (log.isDebugEnabled()) {
                    log.debug("Deleted admin bootstrap credentials file path={}", file);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete admin bootstrap credentials file path={} err={}", file, e.getMessage());
        }
    }

    private static void tryRestrictPermissions(Path file) {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException ignored) {
            // Windows 等非 POSIX
        } catch (Exception e) {
            log.debug("Failed to set admin bootstrap credentials file permissions: {}", e.getMessage());
        }
    }
}
