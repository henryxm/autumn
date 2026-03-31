package cn.org.autumn.table.annotation.sql;

import cn.org.autumn.table.annotation.Table;

/**
 * 表存储引擎的语义化枚举（见 {@link Table#engine()}）。
 * <p><b>方言说明：</b>
 * <ul>
 *   <li><b>MySQL / MariaDB</b>：映射为 {@code CREATE TABLE ... ENGINE = &lt;name&gt;}，{@link #getSqlName()} 即引擎名。</li>
 *   <li><b>PostgreSQL</b>：无等价 {@code ENGINE}；接入时可忽略或由方言映射到表空间 / 访问方法（未来扩展）。</li>
 *   <li><b>Oracle / SQL Server</b>：无此概念，建表实现中应忽略。</li>
 * </ul>
 */
public enum Engine {

    /** InnoDB：事务、行锁、外键（MySQL 推荐默认）。 */
    INNODB("InnoDB"),

    /** MyISAM：表锁、无事务。 */
    MYISAM("MyISAM"),

    /** MEMORY：内存表。 */
    MEMORY("MEMORY"),

    /** CSV。 */
    CSV("CSV"),

    /** ARCHIVE。 */
    ARCHIVE("ARCHIVE"),

    /** BLACKHOLE。 */
    BLACKHOLE("BLACKHOLE"),

    /** MRG_MyISAM。 */
    MRG_MYISAM("MRG_MyISAM"),

    /** FEDERATED（需服务端启用）。 */
    FEDERATED("FEDERATED");

    private final String sqlName;

    Engine(String sqlName) {
        this.sqlName = sqlName;
    }

    /**
     * 当前主实现（MySQL 兼容语法）下 {@code ENGINE=} 后的字面量；其他方言勿直接拼接，应经适配。
     */
    public String getSqlName() {
        return sqlName;
    }
}
