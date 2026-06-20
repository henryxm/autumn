package cn.org.autumn.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 无连字符小写 32 位 UUID 工具，用作 {@link cn.org.autumn.entity.UuidBased} 实体的第二主键。
 * <p>
 * <strong>规范形态（入库 / 关联 / API 推荐）</strong>：32 位小写十六进制、无连字符，例如 {@code a1b2c3d4e5f6...}。
 * 外部输入（带连字符、大小写混合）经 {@link #norm(String)} 或 {@link #requireValid(String)} 规范化后再持久化。
 * <p>
 * 自增 {@code Long id} 仅用于后台代码生成 CRUD；业务关联与对外标识使用实体 {@code uuid} 列（本工具输出值）。
 * 长整型第二主键见 {@link Snow}（{@link cn.org.autumn.entity.SnowBased}）。
 * <p>
 * 通常由 {@link cn.org.autumn.service.AutoIdService} 在插入前自动调用；业务亦可显式 {@link #uuid()} 赋值。
 * 实体 {@code @Column}：第二主键与外键 {@code comment} 为 {@code {概念}:说明} 或 {@code {概念}ID:说明}（新代码优先 {@code {概念}}），{@code String} 默认 {@code length = 32}
 * （见 {@code docs/AI_DUAL_KEY.md} §3）。
 *
 * @see cn.org.autumn.entity.UuidBased
 * @see cn.org.autumn.service.AutoIdService
 * @see Snow
 */
public final class Uuid {

    /**
     * 规范 uuid 固定长度（无连字符）。
     */
    public static final int LENGTH = 32;

    private static final Pattern HEX32 = Pattern.compile("^[0-9a-f]{32}$");

    private Uuid() {
    }

    /**
     * 生成下一个 32 位业务主键（无连字符小写十六进制）。
     */
    public static String uuid() {
        return fromJavaUuid(UUID.randomUUID());
    }

    /**
     * 生成 uuid 并截取前 {@code length} 位（1～32）；常用于短令牌、前缀标签。
     */
    public static String prefix(int length) {
        return slice(uuid(), length);
    }

    /**
     * 截取规范 uuid 的前 {@code length} 位；入参先经 {@link #norm(String)}。
     *
     * @return 截取结果；入参无效或 {@code length} 非法时返回 {@code null}
     */
    public static String prefix(String raw, int length) {
        String uuid = norm(raw);
        if (uuid == null)
            return null;
        return slice(uuid, length);
    }

    /**
     * 规范化为入库形态：去首尾空白、小写、去掉连字符。
     *
     * @return 32 位小写 hex；入参为 null / 空白时返回 {@code null}
     */
    public static String norm(String raw) {
        String s = StringUtils.trimToNull(raw);
        return s == null ? null : s.toLowerCase(Locale.ROOT).replace("-", "");
    }

    /**
     * 是否尚未赋值（null / 仅空白），不做格式校验。与 {@link #isValid(String)} 互补。
     */
    public static boolean isUnset(String raw) {
        return StringUtils.isBlank(raw);
    }

    /**
     * 是否为合法规范 uuid（32 位十六进制；接受带连字符或大小写混合的输入）。
     */
    public static boolean isValid(String raw) {
        String uuid = norm(raw);
        return uuid != null && uuid.length() == LENGTH && HEX32.matcher(uuid).matches();
    }

    /**
     * 忽略大小写与连字符比较两个 uuid 是否相同；任一侧无效时返回 {@code false}。
     */
    public static boolean equals(String left, String right) {
        String a = norm(left);
        String b = norm(right);
        if (a == null || b == null || a.length() != LENGTH || b.length() != LENGTH)
            return false;
        return a.equals(b);
    }

    /**
     * 规范化为入库形态；非法时抛出 {@link IllegalArgumentException}。
     *
     * @throws IllegalArgumentException 入参为空或不是合法 uuid
     */
    public static String requireValid(String raw) {
        String uuid = norm(raw);
        if (!isValid(uuid))
            throw new IllegalArgumentException("非法 uuid: " + raw);
        return uuid;
    }

    /**
     * 转为带连字符的大写展示形态（RFC 4122 字符串风格），例如 {@code A1B2C3D4-E5F6-...}。
     * 入参无效时原样返回 {@code raw}（与历史行为兼容）。
     */
    public static String format(String raw) {
        String uuid = norm(raw);
        if (uuid == null || uuid.length() != LENGTH)
            return raw;
        return toDashed(uuid).toUpperCase(Locale.ROOT);
    }

    /**
     * 转为带连字符的小写展示形态。
     * 入参无效时原样返回 {@code raw}。
     */
    public static String formatLower(String raw) {
        String uuid = norm(raw);
        if (uuid == null || uuid.length() != LENGTH)
            return raw;
        return toDashed(uuid);
    }

    /**
     * 将规范 uuid 转为 {@link UUID}；非法时返回 {@code null}。
     */
    public static UUID toJavaUuid(String raw) {
        String uuid = norm(raw);
        if (!isValid(uuid))
            return null;
        return UUID.fromString(toDashed(uuid));
    }

    /**
     * 将 {@link UUID} 转为规范入库字符串（32 位小写、无连字符）。
     */
    public static String fromJavaUuid(UUID id) {
        if (id == null)
            return null;
        return id.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String toDashed(String uuid) {
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32);
    }

    private static String slice(String uuid, int length) {
        if (length < 1 || length > LENGTH)
            return null;
        return uuid.substring(0, length);
    }
}
