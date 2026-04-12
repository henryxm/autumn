package cn.org.autumn.database;

import org.apache.commons.lang.StringUtils;

/**
 * 应用主库类型（优先由 JDBC URL 推断，其次 {@code autumn.database}；部分类型需二者配合，见各枚举常量说明）。
 * <p>
 * 注解驱动建表范围见 {@link #supportsAnnotationTableSync()} 与 {@link AnnotationTableSyncSupport}。
 * <p>
 * PageHelper 5.1.x 方言别名与 {@link #pageHelperDialectName()} 对齐。
 * 团队规范与全库 SQL 纪律见仓库内 {@code docs/AI_DATABASE.md}（相对仓库根）；{@link cn.org.autumn.database.DatabaseHolder#resolveType} 为统一解析入口。
 */
public enum DatabaseType {

    MYSQL,
    MARIADB,
    POSTGRESQL,
    ORACLE,
    SQLSERVER,
    /** SQLite（含 {@code jdbc:sqlite::memory:} 等） */
    SQLITE,
    /** H2 */
    H2,
    /** HyperSQL */
    HSQLDB,
    /** IBM DB2 */
    DB2,
    /** Apache Derby */
    DERBY,
    /** Firebird 3+（JDBC 多为 {@code jdbc:firebirdsql:}） */
    FIREBIRD,
    /** Informix（{@code jdbc:informix-sqli:} 等） */
    INFORMIX,
    /** 达梦 DM（{@code jdbc:dm:}） */
    DAMENG,
    /**
     * 人大金仓 KingbaseES：官方 {@code jdbc:kingbase8:} / {@code jdbc:kingbase86:}（见 KingbaseES JDBC 手册）。
     */
    KINGBASE,
    /**
     * PingCAP TiDB：官方推荐 {@code jdbc:mysql://...}（MySQL 协议）；推断为 {@link #TIDB} 需配置 {@code autumn.database=tidb}
     *（或 {@code pingcap}），或使用生态中的 {@code jdbc:tidb://}。
     */
    TIDB,
    /**
     * OceanBase MySQL 兼容模式：官方 {@code jdbc:oceanbase://...} 且非 Oracle 兼容参数；或用 {@code jdbc:mysql://} 连 OB MySQL 租户时需
     * {@code autumn.database=oceanbase_mysql}（或 {@code ob_mysql} 等别名）。
     */
    OCEANBASE_MYSQL,
    /**
     * OceanBase Oracle 兼容模式：{@code jdbc:oceanbase://...} 且 URL 中含 {@code compatibleMode=oracle} 或 {@code compatible-mode=oracle}，
     * 或配置 {@code autumn.database=oceanbase_oracle}。
     */
    OCEANBASE_ORACLE,
    /** 未识别的配置值 */
    OTHER;

    /**
     * 是否走注解驱动建表（{@code MysqlTableService} + 路由后的 {@link cn.org.autumn.table.platform.RelationalTableOperations}）。
     *
     * @see AnnotationTableSyncSupport
     */
    public boolean supportsAnnotationTableSync() {
        return AnnotationTableSyncSupport.supports(this);
    }

    public boolean isPostgresql() {
        return this == POSTGRESQL;
    }

    public boolean isMariaDb() {
        return this == MARIADB;
    }

    public boolean isOracle() {
        return this == ORACLE;
    }

    public boolean isSqlServer() {
        return this == SQLSERVER;
    }

    public boolean isMysqlFamily() {
        return this == MYSQL || this == MARIADB || this == TIDB || this == OCEANBASE_MYSQL;
    }

    public boolean isSqlite() {
        return this == SQLITE;
    }

    public boolean isH2() {
        return this == H2;
    }

    public boolean isHsqldb() {
        return this == HSQLDB;
    }

    public boolean isDb2() {
        return this == DB2;
    }

    public boolean isDerby() {
        return this == DERBY;
    }

    public boolean isFirebird() {
        return this == FIREBIRD;
    }

    public boolean isInformix() {
        return this == INFORMIX;
    }

    public boolean isDameng() {
        return this == DAMENG;
    }

    public boolean isKingbase() {
        return this == KINGBASE;
    }

    public boolean isTidb() {
        return this == TIDB;
    }

    public boolean isOceanBaseMysql() {
        return this == OCEANBASE_MYSQL;
    }

    public boolean isOceanBaseOracle() {
        return this == OCEANBASE_ORACLE;
    }

    /**
     * PageHelper {@code helper-dialect} 取值（与 5.1.x {@code PageAutoDialect} 注册别名一致）。
     */
    public String pageHelperDialectName() {
        switch (this) {
            case MYSQL:
                return "mysql";
            case MARIADB:
                return "mariadb";
            case POSTGRESQL:
                return "postgresql";
            case ORACLE:
                return "oracle";
            case SQLSERVER:
                return "sqlserver";
            case SQLITE:
                return "sqlite";
            case H2:
                return "h2";
            case HSQLDB:
                return "hsqldb";
            case DB2:
                return "db2";
            case DERBY:
                return "derby";
            case FIREBIRD:
                return "sqlserver2012";
            case INFORMIX:
                return "informix-sqli";
            case DAMENG:
                return "dm";
            case KINGBASE:
                return "postgresql";
            case TIDB:
            case OCEANBASE_MYSQL:
                return "mysql";
            case OCEANBASE_ORACLE:
                return "oracle";
            default:
                return "mysql";
        }
    }

    /**
     * MyBatis-Plus 2.x 全局 {@code GlobalConfiguration#setIdentifierQuote} 用的格式串（{@link String#format}，单占位符为列名）。
     * MP 内置枚举对 H2 的 quote 为 null，会导致 {@code order} 等保留字列不转义，此处按 Autumn 解析到的库类型补全。
     * 返回 {@code null} 表示不注入，由用户自行配置 {@code mybatis-plus.global-config.identifier-quote}。
     */
    public String mybatisPlusIdentifierQuotePattern() {
        if (isPostgresql()) {
            return "\"%s\"";
        }
        if (isMysqlFamily()) {
            return "`%s`";
        }
        // SQLite / H2 / DB2 / Derby：与各自 RuntimeSqlDialect#quote（双引号）及 JDBC 双引号路径一致；勿与 MySQL 反引号混用。
        if (this == SQLITE || this == H2 || this == DB2 || this == DERBY) {
            return "\"%s\"";
        }
        return null;
    }

    /**
     * 解析配置值；无法列举的别名归入 {@link #OTHER}。
     */
    public static DatabaseType fromConfig(String raw) {
        if (StringUtils.isBlank(raw)) {
            return MYSQL;
        }
        String v = raw.trim().toLowerCase();
        switch (v) {
            case "mariadb":
                return MARIADB;
            case "postgresql":
            case "postgres":
                return POSTGRESQL;
            case "oracle":
                return ORACLE;
            case "sqlserver":
            case "mssql":
                return SQLSERVER;
            case "sqlite":
            case "sqlite3":
                return SQLITE;
            case "h2":
                return H2;
            case "hsqldb":
            case "hypersonic":
                return HSQLDB;
            case "db2":
                return DB2;
            case "derby":
            case "apache_derby":
                return DERBY;
            case "firebird":
            case "firebirdsql":
                return FIREBIRD;
            case "informix":
            case "informix-sqli":
                return INFORMIX;
            case "dm":
            case "dameng":
                return DAMENG;
            case "kingbase":
            case "kingbase8":
            case "kingbase86":
            case "kingbasees":
                return KINGBASE;
            case "tidb":
            case "pingcap":
                return TIDB;
            case "oceanbase":
            case "ob":
            case "oceanbase_mysql":
            case "ob_mysql":
                return OCEANBASE_MYSQL;
            case "oceanbase_oracle":
            case "ob_oracle":
                return OCEANBASE_ORACLE;
            case "mysql":
                return MYSQL;
            default:
                return OTHER;
        }
    }
}
