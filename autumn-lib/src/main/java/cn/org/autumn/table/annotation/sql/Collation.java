package cn.org.autumn.table.annotation.sql;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;

/**
 * 排序规则语义（{@link Table#collation()}、{@link Column#collation()}）。
 * <p><b>方言说明：</b>
 * <ul>
 *   <li><b>MySQL / MariaDB</b>：{@code COLLATE} 子句；{@link #getSqlName()} 为官方 collation 名。</li>
 *   <li><b>PostgreSQL</b>：使用 {@code COLLATE &quot;xxx&quot;} 与操作系统/ICU 规则，名称与 MySQL 不同，需方言映射表。</li>
 *   <li><b>SQL Server</b>：{@code COLLATE} 名称独立体系，同样需映射。</li>
 * </ul>
 * {@link #INHERIT} 表示不显式写 COLLATE，由服务器/表默认决定。
 */
public enum Collation {

    /** 省略 COLLATE（表/列均不强制指定）。 */
    INHERIT(""),

    // ---------- utf8mb4 ----------
    /** utf8mb4 通用 CI。 */
    UTF8MB4_GENERAL_CI("utf8mb4_general_ci"),
    UTF8MB4_UNICODE_CI("utf8mb4_unicode_ci"),
    UTF8MB4_UNICODE_520_CI("utf8mb4_unicode_520_ci"),
    UTF8MB4_0900_AI_CI("utf8mb4_0900_ai_ci"),
    UTF8MB4_0900_AS_CI("utf8mb4_0900_as_ci"),
    UTF8MB4_BIN("utf8mb4_bin"),
    UTF8MB4_DE_PB_0900_AI_CI("utf8mb4_de_pb_0900_ai_ci"),
    UTF8MB4_EN_0900_AI_CI("utf8mb4_en_0900_ai_ci"),
    UTF8MB4_JA_0900_AS_CS("utf8mb4_ja_0900_as_cs"),
    UTF8MB4_ZH_0900_AS_CS("utf8mb4_zh_0900_as_cs"),

    // ---------- utf8 / utf8mb3 ----------
    UTF8_GENERAL_CI("utf8_general_ci"),
    UTF8_UNICODE_CI("utf8_unicode_ci"),
    UTF8_UNICODE_520_CI("utf8_unicode_520_ci"),
    UTF8_BIN("utf8_bin"),
    UTF8MB3_GENERAL_CI("utf8mb3_general_ci"),
    UTF8MB3_UNICODE_CI("utf8mb3_unicode_ci"),
    UTF8MB3_BIN("utf8mb3_bin"),

    // ---------- latin1 ----------
    LATIN1_SWEDISH_CI("latin1_swedish_ci"),
    LATIN1_GENERAL_CI("latin1_general_ci"),
    LATIN1_GENERAL_CS("latin1_general_cs"),
    LATIN1_BIN("latin1_bin"),
    LATIN1_GERMAN1_CI("latin1_german1_ci"),
    LATIN1_GERMAN2_CI("latin1_german2_ci"),

    // ---------- ascii ----------
    ASCII_GENERAL_CI("ascii_general_ci"),
    ASCII_BIN("ascii_bin"),

    // ---------- binary ----------
    BINARY("binary"),

    // ---------- gbk / gb2312 / gb18030 ----------
    GBK_CHINESE_CI("gbk_chinese_ci"),
    GBK_BIN("gbk_bin"),
    GB2312_CHINESE_CI("gb2312_chinese_ci"),
    GB18030_CHINESE_CI("gb18030_chinese_ci"),
    GB18030_BIN("gb18030_bin"),

    // ---------- big5 ----------
    BIG5_CHINESE_CI("big5_chinese_ci"),
    BIG5_BIN("big5_bin");

    private final String sqlName;

    Collation(String sqlName) {
        this.sqlName = sqlName;
    }

    /**
     * 主方言（MySQL 兼容）下的 COLLATE 名；{@link #INHERIT} 为空串。
     */
    public String getSqlName() {
        return sqlName;
    }
}
