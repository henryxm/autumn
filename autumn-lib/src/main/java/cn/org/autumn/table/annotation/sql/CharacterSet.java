package cn.org.autumn.table.annotation.sql;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;

/**
 * 字符集语义（{@link Table#charset()}、{@link Column#charset()}）。
 * <p><b>方言说明：</b>
 * <ul>
 *   <li><b>MySQL / MariaDB</b>：{@code DEFAULT CHARACTER SET}、列上 {@code CHARACTER SET}、{@code CONVERT TO} 等使用 {@link #getSqlName()}。</li>
 *   <li><b>PostgreSQL</b>：编码多为库级；列级语义需在方言层映射为 {@code ENCODING} 或校验与库一致，不可直接拼 MySQL 名称。</li>
 *   <li><b>SQL Server</b>：常用排序规则由 collation 表达；字符集与 PG 类似需映射。</li>
 * </ul>
 * 表级请勿使用 {@link #INHERIT}；误用时框架按 {@link #UTF8} 处理。列级 {@link #INHERIT} 表示不显式声明字符集、继承表默认。
 */
public enum CharacterSet {

    /**
     * 仅用于列：不在 DDL 中生成 CHARACTER SET，与表默认一致。
     */
    INHERIT(""),

    /** UTF-8（MySQL 中 historic utf8 / utf8mb3，三字节，无 Emoji）。 */
    UTF8("utf8"),

    /** UTF-8 完整四字节（含 Emoji）。 */
    UTF8MB4("utf8mb4"),

    /** Latin-1。 */
    LATIN1("latin1"),

    /** ASCII。 */
    ASCII("ascii"),

    /** Binary。 */
    BINARY("binary"),

    /** GBK。 */
    GBK("gbk"),

    /** GB18030。 */
    GB18030("gb18030"),

    /** UTF-16。 */
    UTF16("utf16"),

    /** UTF-32。 */
    UTF32("utf32"),

    /** UCS-2。 */
    UCS2("ucs2"),

    /** Windows-1250。 */
    CP1250("cp1250"),

    /** Windows-1251。 */
    CP1251("cp1251"),

    /** Windows-1256。 */
    CP1256("cp1256"),

    /** Windows-1257。 */
    CP1257("cp1257"),

    /** KOI8-R。 */
    KOI8R("koi8r"),

    /** KOI8-U。 */
    KOI8U("koi8u"),

    /** Mac Roman。 */
    MACROMAN("macroman"),

    /** Swedish 7-bit。 */
    SWE7("swe7"),

    /** HP Western European。 */
    HP8("hp8"),

    /** DEC Western European。 */
    DEC8("dec8"),

    /** ARMSCII-8。 */
    ARMSCII8("armscii8"),

    /** GEOSTD8。 */
    GEOSTD8("geostd8"),

    /** Greek。 */
    GREEK("greek"),

    /** Hebrew。 */
    HEBREW("hebrew"),

    /** KEYBCS2。 */
    KEYBCS2("keybcs2"),

    /** Mac Central European。 */
    MACCE("macce"),

    /** TIS-620。 */
    TIS620("tis620"),

    /** EUC-KR。 */
    EUCKR("euckr"),

    /** EUC-JP（ujis）。 */
    UJIS("ujis"),

    /** Shift_JIS。 */
    SJIS("sjis"),

    /** eucJPms。 */
    EUCJPMS("eucjpms"),

    /** Big5。 */
    BIG5("big5"),

    /** GB2312。 */
    GB2312("gb2312"),

    /** CP932。 */
    CP932("cp932"),

    /** CP850。 */
    CP850("cp850"),

    /** CP852。 */
    CP852("cp852"),

    /** CP866。 */
    CP866("cp866");

    private final String sqlName;

    CharacterSet(String sqlName) {
        this.sqlName = sqlName;
    }

    /**
     * 主方言（MySQL 兼容）下使用的字符集标识符；{@link #INHERIT} 为空串。
     * 非 MySQL 方言实现应通过映射表转换，勿假定名称全局可用。
     */
    public String getSqlName() {
        return sqlName;
    }

    public boolean isInherit() {
        return this == INHERIT;
    }
}
