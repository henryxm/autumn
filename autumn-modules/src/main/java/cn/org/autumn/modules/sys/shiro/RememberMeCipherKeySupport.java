package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.utils.RedisKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * RememberMe AES 密钥解析（集群 / 单机 / Docker 自愈）。
 * <p>
 * 优先级：
 * <ol>
 *   <li>SysConfig {@code SECURITY_SESSION_CONFIG.rememberMeCipherKey}（调用方传入）</li>
 *   <li>Redis 共享键（{@link RedisKeys#getRememberMeCipherKey}，SETNX，无 TTL）</li>
 *   <li>本地文件（默认 {@code autumn.node.home}/security/remember-me.key；旧 {@code config/remember-me.key} 迁移）</li>
 *   <li>进程内临时密钥（仅兜底）</li>
 * </ol>
 * 删除本地文件后：有 Redis 则回写恢复；无 Redis 则重新生成（旧 Cookie 失效）。禁止公开默认钥。
 * <p>
 * 不落在节点 uuid 子目录：uuid 存于 {@code node-profile.json}；集群共享钥按 namespace 对齐 Redis，
 * 不应按节点 uuid 分叉。Docker 挂载整个 {@code autumn.node.home} 即可重启保留。
 */
public final class RememberMeCipherKeySupport {

    private static final Logger log = LoggerFactory.getLogger(RememberMeCipherKeySupport.class);

    /** 历史上流传的 guns/renren 默认钥，禁止再使用 */
    public static final String FORBIDDEN_LEGACY_BASE64 = "Z3VucwAAAAAAAAAAAAAAAA==";

    /** 旧默认（相对进程 cwd，Docker 重启易丢） */
    public static final String LEGACY_FILE = "config/remember-me.key";

    /** 相对节点 home 的持久化相对路径 */
    public static final String RELATIVE_UNDER_HOME = "security/remember-me.key";

    private RememberMeCipherKeySupport() {
    }

    /**
     * 解析落盘路径：显式 {@code key-file} &gt; {@code {nodeHome}/security/remember-me.key}；
     * 若目标不存在而旧 {@code config/remember-me.key} 存在，则迁移复制。
     *
     * @param explicitKeyFile 配置的 key-file，空表示自动
     * @param nodeHome        节点画像目录，可为 null（回退 user.home/.autumn）
     */
    public static Path resolveKeyFile(String explicitKeyFile, Path nodeHome) {
        Path target;
        if (StringUtils.isNotBlank(explicitKeyFile)
                && !LEGACY_FILE.equals(explicitKeyFile.trim())
                && !RELATIVE_UNDER_HOME.equals(explicitKeyFile.trim())) {
            target = Paths.get(explicitKeyFile.trim()).toAbsolutePath().normalize();
        } else if (StringUtils.isNotBlank(explicitKeyFile) && LEGACY_FILE.equals(explicitKeyFile.trim())) {
            // 仍允许显式指定旧相对路径
            target = Paths.get(LEGACY_FILE).toAbsolutePath().normalize();
        } else {
            Path home = nodeHome != null
                    ? nodeHome.toAbsolutePath().normalize()
                    : Path.of(System.getProperty("user.home", "."), ".autumn").toAbsolutePath().normalize();
            target = home.resolve(RELATIVE_UNDER_HOME).toAbsolutePath().normalize();
        }
        migrateLegacyIfNeeded(target);
        return target;
    }

    /**
     * @param configuredBase64 SysConfig 中的 Base64 密钥（可空）
     * @param keyFile          已由 {@link #resolveKeyFile} 解析的路径
     * @param redis            可选；集群共享
     * @param namespace         与 Session / ForceLogout 同一命名空间
     */
    public static byte[] resolveCipherKey(String configuredBase64, Path keyFile,
                                          StringRedisTemplate redis, String namespace) {
        Path path = keyFile != null ? keyFile : resolveKeyFile(null, null);
        String cfg = StringUtils.trimToEmpty(configuredBase64);
        if (StringUtils.isNotBlank(cfg) && !FORBIDDEN_LEGACY_BASE64.equals(cfg)) {
            byte[] decoded = tryDecode16(cfg);
            if (decoded != null) {
                // SysConfig 与 Redis 已有值冲突：以 Redis 为准，避免滚动发布串钥
                String redisExisting = readRedisRaw(redis, namespace);
                if (StringUtils.isNotBlank(redisExisting) && !cfg.equals(redisExisting)) {
                    byte[] redisWon = tryDecode16(redisExisting);
                    if (redisWon != null) {
                        log.error("RememberMe cipher-key 与 Redis 不一致，集群以 Redis 为准。请在 SysConfig SECURITY_SESSION_CONFIG 统一密钥或清空 Redis 键后全量滚动");
                        writeLocalQuietly(path, redisExisting);
                        return redisWon;
                    }
                }
                publishToRedisIfAbsent(redis, namespace, cfg);
                writeLocalQuietly(path, cfg.trim());
                return decoded;
            }
            log.warn("RememberMe cipher-key 无效（需 Base64 解码后恰好 16 字节），继续尝试 Redis/文件");
        } else if (FORBIDDEN_LEGACY_BASE64.equals(cfg)) {
            log.warn("拒绝使用公开默认 RememberMe 密钥，继续尝试 Redis/文件");
        }

        String redisB64 = readRedisRaw(redis, namespace);
        if (StringUtils.isNotBlank(redisB64)) {
            byte[] decoded = tryDecode16(redisB64);
            if (decoded != null) {
                // 自愈：本地文件缺失或与集群不一致时回写
                writeLocalQuietly(path, redisB64);
                log.debug("RememberMe 密钥已从 Redis 加载（集群共享），并同步到 {}", path);
                return decoded;
            }
        }

        try {
            if (Files.isRegularFile(path)) {
                String fromFile = StringUtils.trimToEmpty(Files.readString(path, StandardCharsets.UTF_8));
                if (StringUtils.isNotBlank(fromFile) && !FORBIDDEN_LEGACY_BASE64.equals(fromFile)) {
                    byte[] decoded = tryDecode16(fromFile);
                    if (decoded != null) {
                        boolean shared = publishToRedisIfAbsent(redis, namespace, fromFile.trim());
                        if (redis != null && !shared) {
                            String won = readRedisRaw(redis, namespace);
                            if (StringUtils.isNotBlank(won)) {
                                byte[] redisWon = tryDecode16(won);
                                if (redisWon != null) {
                                    log.warn("本地 remember-me.key 与 Redis 不一致，已采用 Redis 密钥。建议统一 Env 或删除分叉本地文件");
                                    writeLocalQuietly(path, won.trim());
                                    return redisWon;
                                }
                            }
                        }
                        if (redis == null) {
                            log.info("RememberMe 密钥来自本地文件 {}（单机/无 Redis）", path);
                        }
                        return decoded;
                    }
                }
            }

            // 文件不存在或损坏：自愈生成
            byte[] key = new byte[16];
            new SecureRandom().nextBytes(key);
            String b64 = Base64.getEncoder().encodeToString(key);

            boolean published = publishToRedisIfAbsent(redis, namespace, b64);
            if (redis != null && !published) {
                String won = readRedisRaw(redis, namespace);
                if (StringUtils.isNotBlank(won)) {
                    byte[] redisWon = tryDecode16(won);
                    if (redisWon != null) {
                        writeLocalQuietly(path, won.trim());
                        log.info("RememberMe 密钥已由集群其他节点写入 Redis，本节点采用共享密钥并回写 {}", path);
                        return redisWon;
                    }
                }
            }
            writeLocalQuietly(path, b64);
            if (redis != null) {
                log.info("已生成 RememberMe 密钥并写入 Redis + {}", path);
            } else {
                log.info("已生成 RememberMe 密钥并写入 {}（删除该文件后重启将重新生成，旧记住我 Cookie 失效）", path);
            }
            return key;
        } catch (Exception e) {
            log.error("RememberMe 密钥文件/Redis 处理失败，使用进程内临时密钥（重启即变）: {}", e.getMessage());
            byte[] fallback = new byte[16];
            new SecureRandom().nextBytes(fallback);
            return fallback;
        }
    }

    /** 兼容：字符串路径、无 Redis */
    public static byte[] resolveCipherKey(String configuredBase64, String filePath) {
        Path path = StringUtils.isNotBlank(filePath)
                ? Paths.get(filePath).toAbsolutePath().normalize()
                : resolveKeyFile(null, null);
        return resolveCipherKey(configuredBase64, path, null, null);
    }

    public static byte[] resolveCipherKey(String configuredBase64, String filePath,
                                          StringRedisTemplate redis, String namespace) {
        Path path = StringUtils.isNotBlank(filePath)
                ? resolveKeyFile(filePath, null)
                : resolveKeyFile(null, null);
        // 若传入的是显式绝对/自定义路径，resolveKeyFile 对非默认会原样使用
        if (StringUtils.isNotBlank(filePath)
                && !LEGACY_FILE.equals(filePath.trim())
                && !RELATIVE_UNDER_HOME.equals(filePath.trim())) {
            path = Paths.get(filePath.trim()).toAbsolutePath().normalize();
            migrateLegacyIfNeeded(path);
        }
        return resolveCipherKey(configuredBase64, path, redis, namespace);
    }

    private static void migrateLegacyIfNeeded(Path target) {
        try {
            if (Files.isRegularFile(target)) {
                return;
            }
            Path legacy = Paths.get(LEGACY_FILE).toAbsolutePath().normalize();
            if (!Files.isRegularFile(legacy) || legacy.equals(target)) {
                return;
            }
            String body = StringUtils.trimToEmpty(Files.readString(legacy, StandardCharsets.UTF_8));
            if (StringUtils.isBlank(body) || FORBIDDEN_LEGACY_BASE64.equals(body)) {
                return;
            }
            if (tryDecode16(body) == null) {
                return;
            }
            writeLocalQuietly(target, body.trim());
            log.info("已将旧 RememberMe 密钥从 {} 迁移到 {}", legacy, target);
        } catch (Exception e) {
            log.warn("迁移旧 remember-me.key 失败: {}", e.getMessage());
        }
    }

    private static void writeLocalQuietly(Path path, String b64) {
        if (path == null || StringUtils.isBlank(b64)) {
            return;
        }
        try {
            if (Files.isRegularFile(path)) {
                String existing = StringUtils.trimToEmpty(Files.readString(path, StandardCharsets.UTF_8));
                if (b64.equals(existing)) {
                    return;
                }
            }
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, b64 + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            log.warn("写入 RememberMe 密钥文件失败 path={}: {}", path, e.getMessage());
        }
    }

    private static String readRedisRaw(StringRedisTemplate redis, String namespace) {
        if (redis == null) {
            return null;
        }
        try {
            String v = redis.opsForValue().get(RedisKeys.getRememberMeCipherKey(namespace));
            if (StringUtils.isBlank(v) || FORBIDDEN_LEGACY_BASE64.equals(v.trim())) {
                return null;
            }
            return v.trim();
        } catch (Exception e) {
            log.warn("读取 Redis RememberMe 密钥失败: {}", e.getMessage());
            return null;
        }
    }

    private static boolean publishToRedisIfAbsent(StringRedisTemplate redis, String namespace, String b64) {
        if (redis == null || StringUtils.isBlank(b64)) {
            return false;
        }
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(RedisKeys.getRememberMeCipherKey(namespace), b64.trim());
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("写入 Redis RememberMe 密钥失败: {}", e.getMessage());
            return false;
        }
    }

    private static byte[] tryDecode16(String b64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(b64);
            if (decoded != null && decoded.length == 16) {
                return decoded;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
