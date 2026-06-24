package cn.org.autumn.crypto;

import lombok.Getter;

/**
 * {@code @Cache} 回源时的 DB 等值条件（列名 + 绑定值）。
 * <p>
 * 加密 searchable 字段走盲索引列；hash 列 {@code @Cache} 走 hash 列本身。
 */
@Getter
public class FieldEncryptCacheLookup {

    private final String columnName;
    private final Object queryValue;

    public FieldEncryptCacheLookup(String columnName, Object queryValue) {
        this.columnName = columnName;
        this.queryValue = queryValue;
    }
}
