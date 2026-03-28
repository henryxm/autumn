package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;

/**
 * {@link Column#length()} 注解默认值为 255；在基本类型字段上通常不写 length，若误写错误值，
 * 在此按 MySQL 常见显示宽度/精度规范收敛，避免生成 {@code int(255)} 等非法或怪异 DDL。
 */
public final class ColumnLengthCorrection {

    /**
     * 与 {@link Column#length()} 默认值一致：表示「未显式指定长度」时的占位。
     */
    public static final int ANNOTATION_DEFAULT_LENGTH = 255;

    private ColumnLengthCorrection() {
    }

    /**
     * 是否为注解默认长度或未设置有效值（≤0 视为无效，与旧逻辑兼容）。
     */
    public static boolean isUnsetOrDefault(int length) {
        return length == ANNOTATION_DEFAULT_LENGTH || length <= 0;
    }

    /**
     * {@code INT} 显示宽度通常为 1～11。
     */
    public static int normalizeInt(int length) {
        if (isUnsetOrDefault(length) || length > 11) {
            return 11;
        }
        return length;
    }

    /**
     * {@code BIGINT} 显示宽度通常为 1～20。
     */
    public static int normalizeBigInt(int length) {
        if (isUnsetOrDefault(length) || length > 20) {
            return 20;
        }
        return length;
    }

    /**
     * {@code SMALLINT} 显示宽度通常为 1～6。
     */
    public static int normalizeSmallInt(int length) {
        if (isUnsetOrDefault(length) || length > 6) {
            return 6;
        }
        return length;
    }

    /**
     * 非布尔含义的 {@code TINYINT} 显示宽度通常为 1～4。
     */
    public static int normalizeTinyInt(int length) {
        if (isUnsetOrDefault(length) || length > 4) {
            return 4;
        }
        return length;
    }

    /**
     * {@code FLOAT} 精度位数常见收敛到 11（与历史生成习惯一致）。
     */
    public static int normalizeFloat(int length) {
        if (isUnsetOrDefault(length) || length > 24) {
            return 11;
        }
        return length;
    }

    /**
     * {@code DOUBLE} 总精度第一位常见收敛到 11。
     */
    public static int normalizeDouble(int length) {
        if (isUnsetOrDefault(length) || length > 53) {
            return 11;
        }
        return length;
    }

    /**
     * {@code DECIMAL(M,D)} 的 M：默认 20；超过 MySQL 上限 65 时截断；保证 M &gt; D。
     */
    public static int normalizeDecimalM(int m, int d) {
        int mm = m;
        if (isUnsetOrDefault(mm) || mm > 65) {
            mm = 20;
        }
        if (d > 0 && mm <= d) {
            mm = Math.min(65, d + 10);
        }
        return mm;
    }
}
