package cn.org.autumn.crypto;

import lombok.Getter;

/**
 * {@code @Cache#name()}（naming）+ 缓存键；naming 为空表示默认通道。
 */
@Getter
public class FieldEncryptCacheKey {

    private final String naming;
    private final Object key;

    public FieldEncryptCacheKey(String naming, Object key) {
        this.naming = naming == null ? "" : naming;
        this.key = key;
    }
}
