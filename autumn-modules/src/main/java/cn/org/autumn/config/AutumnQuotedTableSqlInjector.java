package cn.org.autumn.config;

import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.enums.DBType;
import com.baomidou.mybatisplus.enums.IdType;
import com.baomidou.mybatisplus.mapper.LogicSqlInjector;
import com.baomidou.mybatisplus.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * MP 2.x 仅在列命中 {@link com.baomidou.mybatisplus.toolkit.SqlReservedWords} 时用全局 {@code identifier-quote} 转义，
 * 表名始终原样拼进 {@code INSERT/UPDATE/DELETE/SELECT}；Derby/DB2 等会将未加引号的标识符折成大写，与注解 DDL 中
 * {@code "sys_config"} 等小写双引号表不一致。在注入 MappedStatement 前按全局引号模式包装物理表名（与
 * {@link cn.org.autumn.database.DatabaseType#mybatisPlusIdentifierQuotePattern()} / JDBC 推断一致）。
 * <p>
 * {@link com.baomidou.mybatisplus.toolkit.GlobalConfigUtils#setMetaData} 会在 {@code dbType} 为空时按 JDBC URL
 * 推断类型；MP 2.x 的 {@link com.baomidou.mybatisplus.toolkit.JdbcUtils#getDbType} 不含 {@code jdbc:derby:}，Derby
 * 会落成 {@link DBType#OTHER}，{@link com.baomidou.mybatisplus.toolkit.SqlReservedWords} 仅给保留字加引号，
 * {@code param_key} 被 Derby 折成 {@code PARAM_KEY} 与注解双引号 DDL 不一致。{@code ConfigurationCustomizer}
 * 写在 {@link GlobalConfigUtils#DEFAULT} 上的 {@code postgresql} 还会在 {@code signGlobalConfig} 时被 yml 转换的
 * 新 {@link GlobalConfiguration} 覆盖。故在此处、生成 SQL 前对「双引号转义 + 仍为 OTHER/SQLite/H2/HSQL/DB2」
 * 强制 {@code postgresql}，仅影响列名转义策略，不改变实际 JDBC 连接。
 * <p>
 * {@link com.baomidou.mybatisplus.toolkit.TableInfoHelper#initTableInfo} 在 {@link #injectSql} 之前于
 * {@link com.baomidou.mybatisplus.mapper.AutoSqlInjector#inject} 内调用；若彼时全局仍为 {@link DBType#OTHER}，
 * {@link com.baomidou.mybatisplus.entity.TableFieldInfo#setColumn} 只会给保留字加引号，普通列与 DDL 不一致。
 * 故重写 {@link #inject}，在 {@code initTableInfo} 之前先 {@link #ensurePostgresStyleColumnQuotingForQuotedLowercaseDdl}。
 * 另：主键列名由 {@code initTableId} 直接写入，从不走 {@code SqlReservedWords}，需在注入 SQL 前按
 * {@code identifier-quote} 包装（见 {@link #quotePhysicalKeyColumn}）。
 * <p>
 * 自增主键插入走 {@link org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator}，{@code MappedStatement} 上的
 * {@code keyColumn} 会交给 JDBC 取生成键列名；Derby 不接受带双引号的字面名（如 {@code "id"}），故
 * {@link #injectInsertOneSql} 在 {@link IdType#AUTO} 时临时改为 {@link #plainIdentifierForJdbcKeyColumn}。
 */
public class AutumnQuotedTableSqlInjector extends LogicSqlInjector {

    @Override
    protected void injectInsertOneSql(boolean selective, Class<?> mapperClass, Class<?> modelClass, TableInfo table) {
        if (table != null && table.getIdType() == IdType.AUTO && StringUtils.isNotEmpty(table.getKeyColumn())) {
            String orig = table.getKeyColumn();
            String plain = plainIdentifierForJdbcKeyColumn(orig);
            if (!plain.equals(orig)) {
                table.setKeyColumn(plain);
                try {
                    super.injectInsertOneSql(selective, mapperClass, modelClass, table);
                } finally {
                    table.setKeyColumn(orig);
                }
                return;
            }
        }
        super.injectInsertOneSql(selective, mapperClass, modelClass, table);
    }

    @Override
    public void inject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        GlobalConfiguration gc = GlobalConfigUtils.getGlobalConfig(builderAssistant.getConfiguration());
        ensurePostgresStyleColumnQuotingForQuotedLowercaseDdl(gc);
        super.inject(builderAssistant, mapperClass);
    }

    @Override
    protected void injectSql(MapperBuilderAssistant builderAssistant, Class<?> mapperClass, Class<?> modelClass,
                             TableInfo table) {
        if (builderAssistant != null && table != null) {
            GlobalConfiguration gc = GlobalConfigUtils.getGlobalConfig(builderAssistant.getConfiguration());
            ensurePostgresStyleColumnQuotingForQuotedLowercaseDdl(gc);
            quotePhysicalKeyColumn(gc, table);
            applyQuotedTableName(gc, table);
        }
        super.injectSql(builderAssistant, mapperClass, modelClass, table);
    }

    /**
     * MP 2.x 仅 {@link DBType#POSTGRE} 对所有列名做 {@code identifier-quote}；双引号系 DDL 小写列需与此对齐。
     */
    static void ensurePostgresStyleColumnQuotingForQuotedLowercaseDdl(GlobalConfiguration gc) {
        if (gc == null || gc.getDbType() == DBType.POSTGRE) {
            return;
        }
        String pattern = gc.getIdentifierQuote();
        if (StringUtils.isEmpty(pattern) || !pattern.contains("\"")) {
            return;
        }
        DBType t = gc.getDbType();
        if (t == DBType.OTHER || t == DBType.SQLITE || t == DBType.H2 || t == DBType.HSQL || t == DBType.DB2) {
            gc.setDbType("postgresql");
        }
    }

    static void applyQuotedTableName(GlobalConfiguration gc, TableInfo table) {
        if (gc == null) {
            return;
        }
        String pattern = gc.getIdentifierQuote();
        if (StringUtils.isEmpty(pattern) || !pattern.contains("%s")) {
            return;
        }
        String name = table.getTableName();
        if (StringUtils.isEmpty(name)) {
            return;
        }
        char c = name.charAt(0);
        if (c == '"' || c == '`' || c == '[') {
            return;
        }
        table.setTableName(String.format(pattern, name));
    }

    /**
     * {@code sqlSelectColumns} 在 {@code keyRelated} 时对主键段使用 {@link TableInfo#getKeyColumn()} 原样拼接，
     * 不经 {@link com.baomidou.mybatisplus.toolkit.SqlReservedWords}，Derby 会将 {@code user_id} 折成 {@code USER_ID}。
     */
    static void quotePhysicalKeyColumn(GlobalConfiguration gc, TableInfo table) {
        if (gc == null || table == null) {
            return;
        }
        String pattern = gc.getIdentifierQuote();
        if (StringUtils.isEmpty(pattern) || !pattern.contains("%s")) {
            return;
        }
        String kc = table.getKeyColumn();
        if (StringUtils.isEmpty(kc)) {
            return;
        }
        char c = kc.charAt(0);
        if (c == '"' || c == '`' || c == '[') {
            return;
        }
        String esc = kc;
        if (pattern.indexOf('"') >= 0) {
            esc = esc.replace("\"", "\"\"");
        }
        if (pattern.indexOf('`') >= 0) {
            esc = esc.replace("`", "``");
        }
        table.setKeyColumn(String.format(pattern, esc));
    }

    /** JDBC 回填生成键用的列名，不能带 SQL 分隔符。 */
    static String plainIdentifierForJdbcKeyColumn(String sqlIdent) {
        if (StringUtils.isEmpty(sqlIdent)) {
            return sqlIdent;
        }
        int n = sqlIdent.length();
        if (n >= 2 && sqlIdent.charAt(0) == '"' && sqlIdent.charAt(n - 1) == '"') {
            return sqlIdent.substring(1, n - 1).replace("\"\"", "\"");
        }
        if (n >= 2 && sqlIdent.charAt(0) == '`' && sqlIdent.charAt(n - 1) == '`') {
            return sqlIdent.substring(1, n - 1).replace("``", "`");
        }
        return sqlIdent;
    }
}
