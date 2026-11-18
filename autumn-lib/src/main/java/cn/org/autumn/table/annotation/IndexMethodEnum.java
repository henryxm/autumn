package cn.org.autumn.table.annotation;

/**
 * 索引方法。既有 {@link #BTREE}/{@link #HASH} 不得改名；新增常量仅追加在末尾。
 */
public enum IndexMethodEnum {
    BTREE,
    HASH,
    GIN,
    GIST,
    BRIN,
    SPGIST,
    RTREE,
}
