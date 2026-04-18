package cn.org.autumn.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 在 Redisson 与 Spring Data Redis 集成版本错配（或其它有缺陷的连接栈）下，绕过 Java 侧 expire、pExpire
 * 默认委托链，改为在 Redis 服务端通过 Lua 执行 TTL 语义。
 *
 * <p>
 * 现象：{@code java.lang.StackOverflowError}，栈顶多为 Spring Data Redis 兼容层
 * {@code DefaultedRedisConnection} 对 {@code pExpire} 的反复自调用。
 *
 * <p>
 * 根因概要：{@code redisson-spring-data-XX} 与 {@code spring-data-redis} 主版本不一致时，
 * {@code keyCommands} 与兼容层互相委托导致无限递归；原理与 Maven 对齐见仓库文档
 * {@code docs/REDIS_REDISSON_SPRING_DATA.md}。
 *
 * <p>
 * Maven：业务根 POM 对齐 {@code redisson-spring-boot-starter}、{@code redisson-spring-data-XX}。
 * 涉及设置或刷新 TTL 的调用优先使用本类；避免直接走
 * {@code RedisTemplate#expire}、{@code ValueOperations#set(key, value, duration, TimeUnit)} 等易踩集成层的路径。
 *
 * <p>
 * 场景说明、替换顺序与脚本体检见 {@code docs/REDIS_TTL_GUIDE.md}。
 *
 * <p>
 * <b>API 对照</b>（字符串键值；序列化见末段）：
 * <ul>
 * <li>{@code expire(RedisTemplate, String, long, TimeUnit)} — 对已有键设置相对过期时间（秒级精度；Lua {@code EXPIRE}）</li>
 * <li>{@code pExpire(RedisTemplate, String, long)} — 对已有键设置相对过期时间（毫秒级精度；Lua {@code PEXPIRE}）</li>
 * <li>{@code expireAtSeconds(RedisTemplate, String, long)} — 设置绝对过期时间（Unix 秒；Lua {@code EXPIREAT}）</li>
 * <li>{@code pExpireAtMillis(RedisTemplate, String, long)} — 设置绝对过期时间（Unix 毫秒；Lua {@code PEXPIREAT}）</li>
 * <li>{@code setWithExpire} / {@code setWithExpirePx} — 写入字符串值并同时设置过期（Lua {@code SET … EX} / {@code SET … PX}）</li>
 * <li>{@code setIfAbsentWithExpire} / {@code setIfAbsentWithExpirePx} — 仅在键不存在时写入并设置过期（Lua {@code SET … NX EX} / {@code SET … NX PX}）</li>
 * <li>{@code incrementAndExpireIfFirst} / {@code incrementAndPExpireIfFirst} — 计数递增；若为窗口内首次命中则为该键设置滑动窗口 TTL（{@code INCR} 后经本类 {@code expire}/{@code pExpire} 续期）</li>
 * </ul>
 *
 * <p>
 * 本类字符串键值均使用 {@link StringRedisSerializer}；仅适用于字符串键值的 {@link RedisTemplate} 调用约定。
 */
public final class RedisExpireUtil {

    private static final DefaultRedisScript<Long> EXPIRE_SECONDS_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('expire', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<Long> PEXPIRE_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('pexpire', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<Long> EXPIREAT_SECONDS_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('expireat', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<Long> PEXPIREAT_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('pexpireat', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<String> SET_EX_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2])", String.class);

    private static final DefaultRedisScript<String> SET_PX_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2])", String.class);

    /** SET key value NX EX seconds → 返回 1 表示设置成功，0 表示未设置（键已存在）。 */
    private static final DefaultRedisScript<Long> SET_NX_EX_SCRIPT = new DefaultRedisScript<>(
            "local ok = redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX'); if ok then return 1 else return 0 end",
            Long.class);

    /** SET key value NX PX milliseconds */
    private static final DefaultRedisScript<Long> SET_NX_PX_SCRIPT = new DefaultRedisScript<>(
            "local ok = redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX'); if ok then return 1 else return 0 end",
            Long.class);

    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();

    private RedisExpireUtil() {
    }

    // --- 纯过期（已有键） ---

    /**
     * Lua {@code EXPIRE key seconds}。
     */
    @SuppressWarnings("unchecked")
    public static boolean expire(RedisTemplate redisTemplate, String key, long timeout, TimeUnit unit) {
        long seconds = Math.max(1L, unit.toSeconds(timeout));
        Long result = (Long) redisTemplate.execute(EXPIRE_SECONDS_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), String.valueOf(seconds));
        return result != null && result > 0;
    }

    /**
     * Lua {@code PEXPIRE key milliseconds}。
     */
    @SuppressWarnings("unchecked")
    public static boolean pExpire(RedisTemplate redisTemplate, String key, long millis) {
        long ms = Math.max(1L, millis);
        Long result = (Long) redisTemplate.execute(PEXPIRE_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), String.valueOf(ms));
        return result != null && result > 0;
    }

    /**
     * Lua {@code EXPIREAT key unixTimeSeconds}。
     */
    @SuppressWarnings("unchecked")
    public static boolean expireAtSeconds(RedisTemplate redisTemplate, String key, long unixSeconds) {
        Long result = (Long) redisTemplate.execute(EXPIREAT_SECONDS_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), String.valueOf(unixSeconds));
        return result != null && result > 0;
    }

    /**
     * Lua {@code PEXPIREAT key unixTimeMillis}。
     */
    @SuppressWarnings("unchecked")
    public static boolean pExpireAtMillis(RedisTemplate redisTemplate, String key, long unixMillis) {
        Long result = (Long) redisTemplate.execute(PEXPIREAT_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), String.valueOf(unixMillis));
        return result != null && result > 0;
    }

    // --- 写入并过期 ---

    /**
     * Lua {@code SET key value EX seconds}。
     */
    @SuppressWarnings("unchecked")
    public static void setWithExpire(RedisTemplate redisTemplate, String key, String value,
                                     long timeout, TimeUnit unit) {
        long seconds = Math.max(1L, unit.toSeconds(timeout));
        redisTemplate.execute(SET_EX_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), value, String.valueOf(seconds));
    }

    /**
     * Lua {@code SET key value PX milliseconds}。
     */
    @SuppressWarnings("unchecked")
    public static void setWithExpirePx(RedisTemplate redisTemplate, String key, String value, long millis) {
        long px = Math.max(1L, millis);
        redisTemplate.execute(SET_PX_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), value, String.valueOf(px));
    }

    /**
     * Lua SET ... NX EX：仅在键不存在时写入并设置 TTL。
     *
     * @return true 表示本次写入成功（原先不存在）
     */
    @SuppressWarnings("unchecked")
    public static boolean setIfAbsentWithExpire(RedisTemplate redisTemplate, String key, String value,
                                               long timeout, TimeUnit unit) {
        long seconds = Math.max(1L, unit.toSeconds(timeout));
        Long ok = (Long) redisTemplate.execute(SET_NX_EX_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), value, String.valueOf(seconds));
        return ok != null && ok > 0;
    }

    /**
     * Lua {@code SET key value NX PX milliseconds}。
     */
    @SuppressWarnings("unchecked")
    public static boolean setIfAbsentWithExpirePx(RedisTemplate redisTemplate, String key, String value,
                                                 long millis) {
        long px = Math.max(1L, millis);
        Long ok = (Long) redisTemplate.execute(SET_NX_PX_SCRIPT, STRING_SERIALIZER, STRING_SERIALIZER,
                Collections.singletonList(key), value, String.valueOf(px));
        return ok != null && ok > 0;
    }

    // --- 计数 + 窗口首写续期（限流常用） ---

    /**
     * Redis INCR：若计数首次为 1，则为该 key 设置滑动窗口 TTL（Lua EXPIRE）。
     * increment 本身一般不触发问题委托链；续期必须使用本类的 expire 方法。
     */
    @SuppressWarnings("unchecked")
    public static long incrementAndExpireIfFirst(RedisTemplate redisTemplate, String key,
                                                 long windowDuration, TimeUnit unit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && Objects.equals(count, 1L)) {
            expire(redisTemplate, key, windowDuration, unit);
        }
        return count == null ? 0L : count;
    }

    /**
     * 与 incrementAndExpireIfFirst 相同，但滑动窗口以毫秒表示（内部调用 pExpire）。
     */
    @SuppressWarnings("unchecked")
    public static long incrementAndPExpireIfFirst(RedisTemplate redisTemplate, String key, long windowMillis) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && Objects.equals(count, 1L)) {
            pExpire(redisTemplate, key, windowMillis);
        }
        return count == null ? 0L : count;
    }
}
