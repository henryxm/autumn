package cn.org.autumn.database.runtime.query;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.table.annotation.IndexMethodEnum;
import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.index.IndexColumnSpec;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询即索引：同一链路生成可移植 SQL，并登记与 Entity {@code @Index}/{@code @Indexes} 能力对等的 {@link IndexInfo}
 * （含单列前缀/方向/expression/部分索引等）。
 * <p>
 * Provider 方法上标<strong>空</strong>{@code @Index} 作发现白名单；索引定义只认本类链式 API。
 * 启动时 {@link QueryIndexDiscovery} invoke 带标记的无参方法，{@link #sql()} 写入 {@link QueryIndexRegistry}。
 * 与 Entity 同指纹时<strong>以查询即索引为准</strong>。
 * <p>
 * 冻结后 SQL 模板按「方言 + 查询签名」memoize。
 */
public final class IndexedSelect {

    private static final ConcurrentHashMap<String, String> SQL_MEMO = new ConcurrentHashMap<>();

    private final RuntimeSql runtime;
    private final String table;

    private final List<WherePart> whereParts = new ArrayList<>();
    private final List<OrderPart> orderParts = new ArrayList<>();
    private final List<IndexColumnSpec> indexColumns = new ArrayList<>();

    private boolean limitOne;
    private String indexName = "";
    private IndexTypeEnum indexType = IndexTypeEnum.NORMAL;
    private IndexMethodEnum indexMethod = IndexMethodEnum.BTREE;
    private String comment = "";
    private String indexWhere = "";
    private final List<String> includeColumns = new ArrayList<>();
    private String parser = "";
    private boolean invisible;
    private boolean registerOnSql = true;

    private IndexedSelect(RuntimeSql runtime, String table) {
        this.runtime = runtime == null ? RuntimeSql.sql : runtime;
        this.table = table == null ? "" : table.trim();
    }

    public static IndexedSelect from(RuntimeSql runtime, String table) {
        return new IndexedSelect(runtime, table);
    }

    /** 等值条件，并作为索引键（ASC）。MyBatis 参数名保留调用方传入的 {@code column} 字面量。 */
    public IndexedSelect eq(String column) {
        return eq(column, 0);
    }

    /** 等值 + 索引前缀长度（对齐 {@code @IndexField(length)}）。 */
    public IndexedSelect eq(String column, int prefixLength) {
        return eq(column, prefixLength, IndexOrder.ASC);
    }

    /** 等值 + 前缀 + 列方向（对齐 {@code @IndexField(order)}）。 */
    public IndexedSelect eq(String column, int prefixLength, IndexOrder order) {
        String param = requireRaw(column);
        String col = toColumn(param);
        whereParts.add(WherePart.eq(col, param));
        addIndexCol(col, prefixLength, order == null ? IndexOrder.ASC : order, "");
        return this;
    }

    /**
     * IN 条件，并作为索引键。产物 {@code IN (#{param})}，须配合 Dao
     * {@code @Lang(SelectInLangDriver.class)}。
     */
    public IndexedSelect in(String column) {
        return in(column, 0);
    }

    public IndexedSelect in(String column, int prefixLength) {
        return in(column, prefixLength, IndexOrder.ASC);
    }

    public IndexedSelect in(String column, int prefixLength, IndexOrder order) {
        String param = requireRaw(column);
        String col = toColumn(param);
        whereParts.add(WherePart.in(col, param));
        addIndexCol(col, prefixLength, order == null ? IndexOrder.ASC : order, "");
        return this;
    }

    /**
     * 范围谓词（{@code >=}；更复杂条件用 {@link #rawWhere}）；
     * 登记为索引键 ASC。
     */
    public IndexedSelect range(String column) {
        return range(column, 0);
    }

    public IndexedSelect range(String column, int prefixLength) {
        return range(column, prefixLength, IndexOrder.ASC);
    }

    public IndexedSelect range(String column, int prefixLength, IndexOrder order) {
        String param = requireRaw(column);
        String col = toColumn(param);
        whereParts.add(WherePart.rangeGe(col, param));
        addIndexCol(col, prefixLength, order == null ? IndexOrder.ASC : order, "");
        return this;
    }

    /**
     * 设置<strong>最后一列</strong>索引 collation（对齐 {@code @IndexField(collation)}）。
     */
    public IndexedSelect collation(String collation) {
        if (indexColumns.isEmpty()) {
            throw new IllegalStateException("collation() requires a prior index column (eq/in/range/col/orderBy)");
        }
        String c = collation == null ? "" : collation.trim();
        int last = indexColumns.size() - 1;
        IndexColumnSpec spec = indexColumns.get(last);
        if (spec.isExpression()) {
            return this;
        }
        indexColumns.set(last, IndexColumnSpec.ofColumn(spec.getColumn(), spec.getPrefixLength(), spec.getOrder(), c));
        return this;
    }

    /** 设置最后一列索引前缀长度（对齐 {@code @IndexField(length)}）。 */
    public IndexedSelect prefix(int length) {
        if (indexColumns.isEmpty()) {
            throw new IllegalStateException("prefix() requires a prior index column");
        }
        int last = indexColumns.size() - 1;
        IndexColumnSpec spec = indexColumns.get(last);
        if (spec.isExpression()) {
            return this;
        }
        indexColumns.set(last, spec.withPrefixLength(Math.max(0, length)));
        return this;
    }

    /** 显式追加索引列（可不进入 WHERE）。 */
    public IndexedSelect col(String column) {
        return col(column, IndexOrder.ASC, 0, "");
    }

    public IndexedSelect col(String column, IndexOrder order) {
        return col(column, order, 0, "");
    }

    public IndexedSelect col(String column, IndexOrder order, int prefixLength) {
        return col(column, order, prefixLength, "");
    }

    public IndexedSelect col(String column, IndexOrder order, int prefixLength, String collation) {
        String col = toColumn(requireRaw(column));
        addIndexCol(col, prefixLength, order == null ? IndexOrder.ASC : order,
                collation == null ? "" : collation.trim());
        return this;
    }

    /** 表达式索引列（只进索引，不进 WHERE）。 */
    public IndexedSelect expression(String expr) {
        return expression(expr, IndexOrder.ASC);
    }

    public IndexedSelect expression(String expr, IndexOrder order) {
        if (StringUtils.isBlank(expr)) {
            throw new IllegalArgumentException("expression required");
        }
        indexColumns.add(IndexColumnSpec.ofExpression(expr.trim(), order == null ? IndexOrder.ASC : order));
        return this;
    }

    public IndexedSelect orderByAsc(String column) {
        return orderBy(column, IndexOrder.ASC, 0);
    }

    public IndexedSelect orderByDesc(String column) {
        return orderBy(column, IndexOrder.DESC, 0);
    }

    public IndexedSelect orderByDesc(String column, int prefixLength) {
        return orderBy(column, IndexOrder.DESC, prefixLength);
    }

    public IndexedSelect orderBy(String column, IndexOrder order, int prefixLength) {
        String col = toColumn(requireRaw(column));
        IndexOrder o = order == null ? IndexOrder.ASC : order;
        orderParts.add(new OrderPart(col, o));
        addIndexCol(col, prefixLength, o, "");
        return this;
    }

    /** 追加原始 WHERE 片段（不自动进索引；需索引时再用 {@link #col}）。 */
    public IndexedSelect rawWhere(String sqlFragment) {
        if (StringUtils.isNotBlank(sqlFragment)) {
            whereParts.add(WherePart.raw(sqlFragment.trim()));
        }
        return this;
    }

    public IndexedSelect unique() {
        this.indexType = IndexTypeEnum.UNIQUE;
        return this;
    }

    public IndexedSelect indexType(IndexTypeEnum type) {
        if (type != null) {
            this.indexType = type;
        }
        return this;
    }

    public IndexedSelect method(IndexMethodEnum method) {
        if (method != null) {
            this.indexMethod = method;
        }
        return this;
    }

    public IndexedSelect name(String name) {
        this.indexName = name == null ? "" : name.trim();
        return this;
    }

    public IndexedSelect comment(String comment) {
        this.comment = comment == null ? "" : comment;
        return this;
    }

    /** 部分索引 WHERE（对齐 {@code @Index(where)}）。 */
    public IndexedSelect indexWhere(String wherePredicate) {
        this.indexWhere = wherePredicate == null ? "" : wherePredicate;
        return this;
    }

    public IndexedSelect include(String... columns) {
        if (columns != null) {
            for (String c : columns) {
                if (StringUtils.isNotBlank(c)) {
                    includeColumns.add(HumpConvert.HumpToUnderline(c.trim()));
                }
            }
        }
        return this;
    }

    public IndexedSelect parser(String parser) {
        this.parser = parser == null ? "" : parser.trim();
        return this;
    }

    public IndexedSelect invisible(boolean invisible) {
        this.invisible = invisible;
        return this;
    }

    public IndexedSelect limitOne() {
        this.limitOne = true;
        return this;
    }

    /** {@code sql()} 时是否登记索引；默认 true。 */
    public IndexedSelect registerOnSql(boolean register) {
        this.registerOnSql = register;
        return this;
    }

    /**
     * 仅登记索引，不生成 SQL。
     * 已 {@link QueryIndexRegistry#freezeAfterBootstrap()} 时 no-op，且不建 {@link IndexInfo}。
     */
    public IndexedSelect registerIndex() {
        if (QueryIndexRegistry.isFrozen()) {
            return this;
        }
        QueryIndexRegistry.register(table, toIndexInfo());
        return this;
    }

    /**
     * 生成 SELECT * SQL；若 {@link #registerOnSql(boolean)} 为 true 且未冻结则登记索引。
     * 冻结后按方言 + 签名 memoize。
     */
    public String sql() {
        if (registerOnSql && !QueryIndexRegistry.isFrozen()) {
            registerIndex();
        }
        if (QueryIndexRegistry.isFrozen()) {
            return SQL_MEMO.computeIfAbsent(sqlMemoKey(), k -> buildSql());
        }
        return buildSql();
    }

    public static void clearSqlMemoForTests() {
        SQL_MEMO.clear();
    }

    private String sqlMemoKey() {
        RuntimeSqlDialect d = runtime.dialect();
        StringBuilder kb = new StringBuilder(96);
        kb.append(d == null ? "null" : d.getClass().getName()).append('\u0001');
        kb.append(table).append('\u0001');
        for (WherePart w : whereParts) {
            kb.append(w.kind.name()).append(':').append(w.sqlColumn).append(':').append(w.paramName).append('\u0001');
        }
        kb.append('\u0002');
        for (OrderPart o : orderParts) {
            kb.append(o.column).append(':').append(o.order.name()).append('\u0001');
        }
        kb.append('\u0002').append(limitOne ? '1' : '0');
        return kb.toString();
    }

    /** 当前计划对应的 {@link IndexInfo}（不登记）。 */
    public IndexInfo toIndexInfo() {
        IndexInfo info = new IndexInfo();
        info.setName(indexName);
        info.setIndexType(indexType.name());
        info.setIndexMethod(indexMethod.name());
        info.setComment(comment);
        info.setWherePredicate(indexWhere);
        info.setParser(parser);
        info.setInvisible(invisible);
        info.setIncludeColumns(new ArrayList<>(includeColumns));
        info.setTable(table);
        for (IndexColumnSpec spec : indexColumns) {
            info.addColumnSpec(spec);
        }
        return info;
    }

    private String buildSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(runtime.quote(table));
        if (!whereParts.isEmpty()) {
            sb.append(" WHERE ");
            for (int i = 0; i < whereParts.size(); i++) {
                if (i > 0) {
                    sb.append(" AND ");
                }
                sb.append(whereParts.get(i).toSql(runtime));
            }
        }
        if (!orderParts.isEmpty()) {
            sb.append(" ORDER BY ");
            for (int i = 0; i < orderParts.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                OrderPart o = orderParts.get(i);
                sb.append(runtime.quote(o.column)).append(' ').append(o.order.sql());
            }
        }
        if (limitOne) {
            sb.append(runtime.limitOne());
        }
        return sb.toString();
    }

    private void addIndexCol(String underlineCol, int prefixLength, IndexOrder order, String collation) {
        for (IndexColumnSpec existing : indexColumns) {
            if (!existing.isExpression() && underlineCol.equals(existing.getColumn())) {
                return;
            }
        }
        indexColumns.add(IndexColumnSpec.ofColumn(underlineCol, Math.max(0, prefixLength), order, collation));
    }

    private static String requireRaw(String column) {
        if (StringUtils.isBlank(column)) {
            throw new IllegalArgumentException("column required");
        }
        return column.trim();
    }

    private static String toColumn(String raw) {
        return HumpConvert.HumpToUnderline(raw);
    }

    private static final class OrderPart {
        final String column;
        final IndexOrder order;

        OrderPart(String column, IndexOrder order) {
            this.column = column;
            this.order = order;
        }
    }

    private static final class WherePart {
        enum Kind {EQ, IN, RANGE_GE, RAW}

        final Kind kind;
        final String sqlColumn;
        final String paramName;

        private WherePart(Kind kind, String sqlColumn, String paramName) {
            this.kind = kind;
            this.sqlColumn = sqlColumn;
            this.paramName = paramName;
        }

        static WherePart eq(String col, String param) {
            return new WherePart(Kind.EQ, col, param);
        }

        static WherePart in(String col, String param) {
            return new WherePart(Kind.IN, col, param);
        }

        static WherePart rangeGe(String col, String param) {
            return new WherePart(Kind.RANGE_GE, col, param);
        }

        static WherePart raw(String fragment) {
            return new WherePart(Kind.RAW, fragment, fragment);
        }

        String toSql(RuntimeSql runtime) {
            switch (kind) {
                case EQ:
                    return runtime.quote(sqlColumn) + " = #{" + paramName + "}";
                case IN:
                    return runtime.quote(sqlColumn) + " IN (#{" + paramName + "})";
                case RANGE_GE:
                    return runtime.quote(sqlColumn) + " >= #{" + paramName + "}";
                case RAW:
                default:
                    return sqlColumn;
            }
        }
    }
}
