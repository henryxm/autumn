package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.index.IndexColumnSpec;
import cn.org.autumn.table.data.index.IndexTableSettings;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

public class IndexInfo {
    String name;
    /** 有序：列名 → 前缀长度（兼容视图，与 {@link #columnSpecs} 同步）。 */
    Map<String, Integer> fields;
    List<IndexColumnSpec> columnSpecs;
    String indexType;
    String indexMethod;
    String comment;
    String table;
    String nonUnique;
    String keyName;
    String seqInIndex;
    String columnName;
    String collation;
    String cardinality;
    String subPart;
    String packed;
    String indexComment;
    String wherePredicate = "";
    List<String> includeColumns = new ArrayList<>();
    String parser = "";
    boolean invisible;
    /**
     * 预检标记：为 true 时 {@link cn.org.autumn.table.data.index.IndexDialectCapability#forEmit}
     * 返回 null，跳过 ADD DDL（不阻断启动）。
     */
    boolean skipEmit;

    public IndexInfo() {
        fields = new LinkedHashMap<>();
        columnSpecs = new ArrayList<>();
    }

    public void resolve() {
        // SHOW INDEX 的 Index_type 常为 BTREE/HASH（方法），勿在判唯一前丢失
        if ((indexMethod == null || indexMethod.isEmpty()) && indexType != null) {
            if ("BTREE".equalsIgnoreCase(indexType) || "HASH".equalsIgnoreCase(indexType)) {
                indexMethod = indexType.toUpperCase(Locale.ROOT);
            }
        }
        if (isUniqueNonUniqueFlag(nonUnique)) {
            indexType = IndexTypeEnum.UNIQUE.name();
        } else if (isExplicitNonUniqueFlag(nonUnique)) {
            // 库侧明确非唯一；保留 FULLTEXT/SPATIAL
            if (!"FULLTEXT".equalsIgnoreCase(indexType) && !"SPATIAL".equalsIgnoreCase(indexType)) {
                indexType = IndexTypeEnum.NORMAL.name();
            }
        } else if ("FULLTEXT".equalsIgnoreCase(indexType)) {
            indexType = IndexTypeEnum.FULLTEXT.name();
        } else if ("SPATIAL".equalsIgnoreCase(indexType)) {
            indexType = IndexTypeEnum.SPATIAL.name();
        } else if (IndexTypeEnum.UNIQUE.name().equalsIgnoreCase(indexType)) {
            // 注解侧已是 UNIQUE，且 Non_unique 未读到：禁止降级成 NORMAL（否则 ADD 丢 UNIQUE，每启重建）
            indexType = IndexTypeEnum.UNIQUE.name();
        } else {
            indexType = IndexTypeEnum.NORMAL.name();
        }
        if (indexMethod == null || indexMethod.isEmpty()) {
            indexMethod = IndexMethodEnum.BTREE.name();
        }
    }

    /** Non_unique 明确表示「非唯一」（1/true）。 */
    static boolean isExplicitNonUniqueFlag(String nonUnique) {
        if (nonUnique == null) {
            return false;
        }
        String v = nonUnique.trim();
        if (v.isEmpty()) {
            return false;
        }
        if ("1".equals(v) || "true".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v)) {
            return true;
        }
        try {
            return Integer.parseInt(v) != 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * SHOW INDEX / JDBC 的 Non_unique 可能是 {@code 0}/{@code 1}、布尔或 true/false 字符串。
     */
    public static boolean isUniqueNonUniqueFlag(String nonUnique) {
        if (nonUnique == null) {
            return false;
        }
        String v = nonUnique.trim();
        if (v.isEmpty()) {
            return false;
        }
        if ("0".equals(v) || "false".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v) || "no".equalsIgnoreCase(v)) {
            return true;
        }
        try {
            return Integer.parseInt(v) == 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static IndexInfo copy(UniqueKeyInfo uniqueKeyInfo) {
        IndexInfo indexInfo = new IndexInfo();
        indexInfo.setFields(uniqueKeyInfo.getFields());
        indexInfo.setCardinality(uniqueKeyInfo.getCardinality());
        indexInfo.setCollation(uniqueKeyInfo.getCollation());
        indexInfo.setColumnName(uniqueKeyInfo.getColumnName());
        indexInfo.setComment(uniqueKeyInfo.getComment());
        indexInfo.setIndexComment(uniqueKeyInfo.getIndexComment());
        indexInfo.setIndexMethod(uniqueKeyInfo.getIndexMethod());
        indexInfo.setIndexType(uniqueKeyInfo.getIndexType());
        indexInfo.setKeyName(uniqueKeyInfo.getKeyName());
        indexInfo.setName(uniqueKeyInfo.getName());
        indexInfo.setNonUnique(uniqueKeyInfo.getNonUnique());
        indexInfo.setPacked(uniqueKeyInfo.getPacked());
        indexInfo.setSeqInIndex(uniqueKeyInfo.getSeqInIndex());
        indexInfo.setSubPart(uniqueKeyInfo.getSubPart());
        indexInfo.setTable(uniqueKeyInfo.getTable());
        return indexInfo;
    }

    /**
     * 浅拷贝定义侧字段（供方言发射降级，避免原地修改实体收集结果）。
     */
    public static IndexInfo copyOf(IndexInfo src) {
        if (src == null) {
            return null;
        }
        IndexInfo i = new IndexInfo();
        i.name = src.name;
        i.keyName = src.keyName;
        i.indexType = src.indexType;
        i.indexMethod = src.indexMethod;
        i.comment = src.comment;
        i.indexComment = src.indexComment;
        i.table = src.table;
        i.nonUnique = src.nonUnique;
        i.wherePredicate = src.getWherePredicate();
        i.parser = src.getParser();
        i.invisible = src.invisible;
        i.includeColumns = new ArrayList<>(src.getIncludeColumns());
        i.skipEmit = src.skipEmit;
        i.setColumnSpecs(src.getColumnSpecs());
        return i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexInfo)) {
            return false;
        }
        IndexInfo that = (IndexInfo) o;
        return sameIndexColumnsAndKind(that)
                && indexNamesEqualIgnoreCase(this.getName(), that.getName());
    }

    /**
     * 索引名规范化：小写并去掉 {@code _}，用于配对（与 {@link #indexNamesEqualIgnoreCase} 对齐）。
     */
    public static String normalizeIndexName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("_", "").toLowerCase(Locale.ROOT).trim();
    }

    /**
     * 关系库索引名是否视为同一逻辑名：忽略大小写与下划线，并认 {@code 表名_逻辑名} 别名。
     */
    public static boolean relationalSchemaScopedIndexNamesMatch(String tableName, String a, String b) {
        if (tableName == null) {
            tableName = "";
        }
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        String na = normalizeIndexName(a);
        String nb = normalizeIndexName(b);
        if (!na.isEmpty() && na.equals(nb)) {
            return true;
        }
        String scopedB = normalizeIndexName(tableName + "_" + b);
        String scopedA = normalizeIndexName(tableName + "_" + a);
        return (!scopedB.isEmpty() && scopedB.equals(na)) || (!scopedA.isEmpty() && scopedA.equals(nb));
    }

    /** 关系库标识符默认上限（MySQL 64）。 */
    public static final int DEFAULT_MAX_INDEX_NAME_LENGTH = 64;

    /**
     * 由列规格派生压缩名（无下划线），供注解 {@code name} 为空时使用。
     */
    public String deriveNameFromColumns() {
        List<IndexColumnSpec> specs = getColumnSpecs();
        if (specs == null || specs.isEmpty()) {
            return "idx";
        }
        StringBuilder sb = new StringBuilder();
        for (IndexColumnSpec s : specs) {
            if (s == null) {
                continue;
            }
            if (s.isExpression()) {
                sb.append("expr");
            } else if (StringUtils.isNotBlank(s.getColumn())) {
                sb.append(s.getColumn().replace("_", ""));
            }
        }
        String raw = sb.toString().toLowerCase(Locale.ROOT);
        return raw.isEmpty() ? "idx" : raw;
    }

    /**
     * 超长名截断：保留前缀可读段 + {@code _} + 8 位稳定 hex；不超长则原样返回。
     */
    public static String truncateIndexName(String name, int maxLen) {
        if (name == null) {
            name = "";
        }
        if (maxLen <= 0) {
            maxLen = DEFAULT_MAX_INDEX_NAME_LENGTH;
        }
        if (name.length() <= maxLen) {
            return name;
        }
        String hash = String.format("%08x", name.hashCode());
        if (maxLen <= 8) {
            return hash.substring(0, maxLen);
        }
        int keep = maxLen - 1 - 8;
        if (keep < 1) {
            return hash.substring(0, maxLen);
        }
        return name.substring(0, keep) + "_" + hash;
    }

    /**
     * 空名派生 + 长度截断。
     *
     * @return true 表示 name 被改写（派生或截断）
     */
    public boolean ensurePhysicalName(int maxLen) {
        boolean changed = false;
        if (StringUtils.isBlank(this.name)) {
            setName(deriveNameFromColumns());
            changed = true;
        }
        String current = this.name == null ? "" : this.name;
        String truncated = truncateIndexName(current, maxLen);
        if (!current.equals(truncated)) {
            setName(truncated);
            changed = true;
        }
        return changed;
    }

    /**
     * 列与类型/方法是否一致。默认<strong>有序</strong>（含 ASC/DESC；前缀见 {@link #columnSpecEqualForSync}）；
     * {@code autumn.table.index.rebuild-on-order-mismatch=false} 时退回仅列集合比对。
     * <p>
     * where / include / parser / invisible：读回常为空，任一侧默认则跳过该维，避免误重建。
     * 已与库一致时返回 true（禁止无意义 DROP+ADD）。
     */
    public boolean sameIndexColumnsAndKind(IndexInfo that) {
        if (that == null) {
            return false;
        }
        if (!indexTypesCompatible(this, that) || !indexMethodsCompatible(this, that)) {
            return false;
        }
        if (!softStringEqual(this.wherePredicate, that.wherePredicate)) {
            return false;
        }
        if (!softStringEqual(this.parser, that.parser)) {
            return false;
        }
        if (this.invisible != that.invisible && that.invisible) {
            return false;
        }
        if (!softIncludeEqual(this.includeColumns, that.includeColumns)) {
            return false;
        }
        if (IndexTableSettings.rebuildOnOrderMismatch()) {
            return orderedColumnsEqual(this.getColumnSpecs(), that.getColumnSpecs());
        }
        return columnSetEqual(this.getColumnSpecs(), that.getColumnSpecs());
    }

    /** 空方法与 BTREE 等价。 */
    public static boolean indexMethodsCompatible(IndexInfo a, IndexInfo b) {
        return normalizeIndexMethod(a == null ? null : a.getIndexMethod())
                .equalsIgnoreCase(normalizeIndexMethod(b == null ? null : b.getIndexMethod()));
    }

    static String normalizeIndexMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return IndexMethodEnum.BTREE.name();
        }
        return method.trim();
    }

    /** 归一化后的类型严格相等（优先 Non_unique，其次 indexType）。 */
    public static boolean indexTypesCompatible(IndexInfo a, IndexInfo b) {
        return effectiveIndexType(a).equalsIgnoreCase(effectiveIndexType(b));
    }

    /**
     * 优先 {@code Non_unique}；否则用已解析的 indexType（SHOW 的 BTREE/HASH 视为 NORMAL）。
     */
    public static String effectiveIndexType(IndexInfo info) {
        if (info == null) {
            return IndexTypeEnum.NORMAL.name();
        }
        if (isUniqueNonUniqueFlag(info.getNonUnique())) {
            return IndexTypeEnum.UNIQUE.name();
        }
        if (isExplicitNonUniqueFlag(info.getNonUnique())) {
            String t = info.getIndexType();
            if ("FULLTEXT".equalsIgnoreCase(t) || "SPATIAL".equalsIgnoreCase(t)) {
                return t.toUpperCase(Locale.ROOT);
            }
            return IndexTypeEnum.NORMAL.name();
        }
        String t = info.getIndexType();
        if (t == null || t.trim().isEmpty() || "BTREE".equalsIgnoreCase(t) || "HASH".equalsIgnoreCase(t)) {
            return IndexTypeEnum.NORMAL.name();
        }
        if ("FULLTEXT".equalsIgnoreCase(t)) {
            return IndexTypeEnum.FULLTEXT.name();
        }
        if ("SPATIAL".equalsIgnoreCase(t)) {
            return IndexTypeEnum.SPATIAL.name();
        }
        if (IndexTypeEnum.UNIQUE.name().equalsIgnoreCase(t)) {
            return IndexTypeEnum.UNIQUE.name();
        }
        if (IndexTypeEnum.NORMAL.name().equalsIgnoreCase(t)) {
            return IndexTypeEnum.NORMAL.name();
        }
        return t.trim();
    }

    private static boolean orderedColumnsEqual(List<IndexColumnSpec> a, List<IndexColumnSpec> b) {
        if (a == null) {
            a = Collections.emptyList();
        }
        if (b == null) {
            b = Collections.emptyList();
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!columnSpecEqualForSync(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 列名 + 方向严格；前缀一侧为 0（整列 / SHOW Sub_part 为空）时与另一侧兼容，
     * 避免 varchar 整列索引与注解 length 反复 DROP+ADD。
     */
    private static boolean columnSpecEqualForSync(IndexColumnSpec a, IndexColumnSpec b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (!Objects.equals(a.identityKey(), b.identityKey())) {
            return false;
        }
        if (a.getOrder() != b.getOrder()) {
            return false;
        }
        int pa = a.getPrefixLength();
        int pb = b.getPrefixLength();
        return pa == pb || pa == 0 || pb == 0;
    }

    private static boolean columnSetEqual(List<IndexColumnSpec> a, List<IndexColumnSpec> b) {
        if (a == null) {
            a = Collections.emptyList();
        }
        if (b == null) {
            b = Collections.emptyList();
        }
        if (a.size() != b.size()) {
            return false;
        }
        Set<String> keys = new HashSet<>();
        for (IndexColumnSpec s : b) {
            keys.add(s.identityKey());
        }
        for (IndexColumnSpec s : a) {
            if (!keys.contains(s.identityKey())) {
                return false;
            }
        }
        return true;
    }

    /** 两侧均非空才严格比较 INCLUDE。 */
    private static boolean softIncludeEqual(List<String> a, List<String> b) {
        boolean aEmpty = a == null || a.isEmpty();
        boolean bEmpty = b == null || b.isEmpty();
        if (aEmpty || bEmpty) {
            return true;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!normalizeIndexFieldKey(a.get(i)).equals(normalizeIndexFieldKey(b.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /** 任一侧为空则视为未读回/未声明，不触发重建。 */
    private static boolean softStringEqual(String a, String b) {
        String aa = a == null ? "" : a.trim();
        String bb = b == null ? "" : b.trim();
        if (aa.isEmpty() || bb.isEmpty()) {
            return true;
        }
        return aa.equals(bb);
    }

    public boolean matchesForRelationalSchemaScopedIndex(String tableName, IndexInfo that) {
        return that != null && sameIndexColumnsAndKind(that)
                && relationalSchemaScopedIndexNamesMatch(tableName, this.getName(), that.getName());
    }

    private static String normalizeIndexFieldKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean indexNamesEqualIgnoreCase(String a, String b) {
        return normalizeIndexName(a).equals(normalizeIndexName(b));
    }

    @Override
    public int hashCode() {
        // 与 equals / sameIndexColumnsAndKind 对齐（不含 comment；扩展属性读回不稳定故不入 hash）
        return Objects.hash(
                getName() == null ? null : getName().toLowerCase(Locale.ROOT),
                getColumnSpecs(),
                getIndexType() == null ? null : getIndexType().toLowerCase(Locale.ROOT),
                getIndexMethod() == null ? null : getIndexMethod().toLowerCase(Locale.ROOT));
    }

    public IndexInfo(Index indexKey) {
        this.name = indexKey.name();
        this.indexMethod = indexKey.indexMethod().toString();
        this.indexType = indexKey.indexType().toString();
        this.comment = indexKey.comment();
        this.wherePredicate = indexKey.where() == null ? "" : indexKey.where();
        this.parser = indexKey.parser() == null ? "" : indexKey.parser();
        this.invisible = indexKey.invisible();
        this.includeColumns = new ArrayList<>();
        if (indexKey.include() != null) {
            for (String inc : indexKey.include()) {
                if (StringUtils.isNotBlank(inc)) {
                    includeColumns.add(HumpConvert.HumpToUnderline(inc.trim()));
                }
            }
        }
        fields = new LinkedHashMap<>();
        columnSpecs = new ArrayList<>();
        if (indexKey.fields().length > 0) {
            for (IndexField indexKeyField : indexKey.fields()) {
                addFromIndexField(indexKeyField);
            }
        } else if (StringUtils.isNotBlank(indexKey.expression())) {
            IndexOrder exprOrder = indexKey.order() == null ? IndexOrder.ASC : indexKey.order();
            addColumnSpec(IndexColumnSpec.ofExpression(indexKey.expression().trim(), exprOrder));
        }
    }

    public IndexInfo(Index index, Field field) {
        fields = new LinkedHashMap<>();
        columnSpecs = new ArrayList<>();
        this.name = index.name();
        this.wherePredicate = index.where() == null ? "" : index.where();
        this.parser = index.parser() == null ? "" : index.parser();
        this.invisible = index.invisible();
        this.includeColumns = new ArrayList<>();
        if (index.include() != null) {
            for (String inc : index.include()) {
                if (StringUtils.isNotBlank(inc)) {
                    includeColumns.add(HumpConvert.HumpToUnderline(inc.trim()));
                }
            }
        }
        this.indexMethod = index.indexMethod().toString();
        this.indexType = index.indexType().toString();
        this.comment = index.comment();
        IndexOrder fieldOrder = index.order() == null ? IndexOrder.ASC : index.order();
        if (index.fields().length > 0) {
            // 显式 fields 优先（少见）；不自动再叠本字段
            for (IndexField indexKeyField : index.fields()) {
                addFromIndexField(indexKeyField);
            }
        } else if (StringUtils.isNotBlank(index.expression())) {
            addColumnSpec(IndexColumnSpec.ofExpression(index.expression().trim(), fieldOrder));
            if (StringUtils.isBlank(this.name) && field != null) {
                this.name = HumpConvert.HumpToUnderline(field.getName());
            }
        } else if (field != null) {
            // 字段级单列快捷：默认索引本字段；前缀仅 @Index(length>0) 显式请求，勿把 Column.length 当索引前缀
            String col = HumpConvert.HumpToUnderline(field.getName());
            if (StringUtils.isBlank(this.name)) {
                this.name = col;
            }
            Column column = field.getAnnotation(Column.class);
            int requested = Math.max(0, index.length());
            String coll = index.collation() == null ? "" : index.collation().trim();
            addColumnSpec(IndexColumnSpec.ofColumn(col,
                    IndexPrefixRules.effectivePrefixLength(field, column, requested), fieldOrder, coll));
        }
    }

    public IndexInfo(Column columnAnn, Field field) {
        fields = new LinkedHashMap<>();
        columnSpecs = new ArrayList<>();
        this.indexMethod = "BTREE";
        this.indexType = "UNIQUE";
        this.comment = columnAnn.comment();
        this.name = HumpConvert.HumpToUnderline(field.getName());
        addColumnSpec(IndexColumnSpec.ofColumn(name,
                IndexPrefixRules.effectivePrefixLength(field, columnAnn, columnAnn.length()), IndexOrder.ASC));
    }

    private void addFromIndexField(IndexField indexKeyField) {
        IndexOrder order = indexKeyField.order() == null ? IndexOrder.ASC : indexKeyField.order();
        if (StringUtils.isNotBlank(indexKeyField.expression())) {
            addColumnSpec(IndexColumnSpec.ofExpression(indexKeyField.expression().trim(), order));
            return;
        }
        String n = HumpConvert.HumpToUnderline(indexKeyField.field());
        String coll = indexKeyField.collation() == null ? "" : indexKeyField.collation().trim();
        addColumnSpec(IndexColumnSpec.ofColumn(n, indexKeyField.length(), order, coll));
    }

    public void addColumnSpec(IndexColumnSpec spec) {
        if (spec == null) {
            return;
        }
        if (columnSpecs == null) {
            columnSpecs = new ArrayList<>();
        }
        if (fields == null) {
            fields = new LinkedHashMap<>();
        }
        columnSpecs.add(spec);
        if (!spec.isExpression() && StringUtils.isNotBlank(spec.getColumn())) {
            fields.put(spec.getColumn(), spec.getPrefixLength());
        }
    }

    /**
     * 追加一列（读回 SHOW INDEX / JDBC 合并用），保持插入顺序。
     */
    public void appendColumn(String columnName, int prefixLength, IndexOrder order) {
        if (StringUtils.isBlank(columnName)) {
            return;
        }
        addColumnSpec(IndexColumnSpec.ofColumn(columnName, prefixLength, order == null ? IndexOrder.ASC : order));
    }

    public void applyPrefixLengthPolicy(Class<?> entityClass) {
        IndexPrefixRules.applyPrefixLengthPolicy(fields, entityClass);
        if (columnSpecs == null || columnSpecs.isEmpty()) {
            rebuildSpecsFromFields();
            return;
        }
        List<IndexColumnSpec> updated = new ArrayList<>();
        for (IndexColumnSpec spec : columnSpecs) {
            if (spec.isExpression()) {
                updated.add(spec);
                continue;
            }
            Integer len = fields.get(spec.getColumn());
            int fixed = len == null ? spec.getPrefixLength() : len;
            updated.add(spec.withPrefixLength(fixed));
        }
        columnSpecs = updated;
    }

    private void rebuildSpecsFromFields() {
        columnSpecs = new ArrayList<>();
        if (fields == null) {
            return;
        }
        for (Map.Entry<String, Integer> e : fields.entrySet()) {
            columnSpecs.add(IndexColumnSpec.ofColumn(e.getKey(), e.getValue() == null ? 0 : e.getValue()));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.keyName = name;
        this.name = name;
    }

    public Map<String, Integer> getFields() {
        if (fields == null) {
            fields = new LinkedHashMap<>();
        }
        return fields;
    }

    public void setFields(Map<String, Integer> fields) {
        if (fields == null) {
            this.fields = new LinkedHashMap<>();
        } else if (fields instanceof LinkedHashMap) {
            this.fields = fields;
        } else {
            this.fields = new LinkedHashMap<>(fields);
        }
        rebuildSpecsFromFields();
    }

    public List<IndexColumnSpec> getColumnSpecs() {
        if (columnSpecs == null || columnSpecs.isEmpty()) {
            rebuildSpecsFromFields();
        }
        return columnSpecs == null ? Collections.emptyList() : columnSpecs;
    }

    public void setColumnSpecs(List<IndexColumnSpec> columnSpecs) {
        this.columnSpecs = columnSpecs == null ? new ArrayList<>() : new ArrayList<>(columnSpecs);
        this.fields = new LinkedHashMap<>();
        for (IndexColumnSpec spec : this.columnSpecs) {
            if (!spec.isExpression() && StringUtils.isNotBlank(spec.getColumn())) {
                fields.put(spec.getColumn(), spec.getPrefixLength());
            }
        }
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getIndexMethod() {
        return indexMethod;
    }

    public void setIndexMethod(String indexMethod) {
        this.indexMethod = indexMethod;
    }

    public String getComment() {
        if (null == comment)
            comment = "";
        return comment;
    }

    public void setComment(String comment) {
        this.indexComment = comment;
        this.comment = comment;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getNonUnique() {
        return nonUnique;
    }

    public void setNonUnique(String nonUnique) {
        if (nonUnique == null) {
            this.nonUnique = null;
            return;
        }
        String v = nonUnique.trim();
        this.nonUnique = v.isEmpty() ? null : v;
    }

    /** MySQL Connector/J 对 Non_unique 常返回 Integer/Boolean。 */
    public void setNonUnique(Boolean nonUnique) {
        if (nonUnique == null) {
            this.nonUnique = null;
        } else {
            this.nonUnique = nonUnique ? "1" : "0";
        }
    }

    public void setNonUnique(Number nonUnique) {
        if (nonUnique == null) {
            this.nonUnique = null;
        } else {
            this.nonUnique = String.valueOf(nonUnique.intValue());
        }
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.name = keyName;
        this.keyName = keyName;
    }

    public String getSeqInIndex() {
        return seqInIndex;
    }

    public void setSeqInIndex(String seqInIndex) {
        this.seqInIndex = seqInIndex;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    public String getCardinality() {
        return cardinality;
    }

    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    public String getSubPart() {
        return subPart;
    }

    public int getSubPartInt() {
        if (null != subPart) {
            try {
                return Integer.valueOf(subPart);
            } catch (Exception e) {

            }
        }
        return 0;
    }

    public void setSubPart(String subPart) {
        this.subPart = subPart;
    }

    public void setSubPart(Number subPart) {
        if (subPart == null) {
            this.subPart = null;
        } else {
            this.subPart = String.valueOf(subPart.intValue());
        }
    }

    public String getPacked() {
        return packed;
    }

    public void setPacked(String packed) {
        this.packed = packed;
    }

    public String getIndexComment() {
        return indexComment;
    }

    public void setIndexComment(String indexComment) {
        this.comment = indexComment;
        this.indexComment = indexComment;
    }

    public String getWherePredicate() {
        return wherePredicate == null ? "" : wherePredicate;
    }

    public void setWherePredicate(String wherePredicate) {
        this.wherePredicate = wherePredicate == null ? "" : wherePredicate;
    }

    public List<String> getIncludeColumns() {
        return includeColumns == null ? Collections.emptyList() : includeColumns;
    }

    public void setIncludeColumns(List<String> includeColumns) {
        this.includeColumns = includeColumns == null ? new ArrayList<>() : new ArrayList<>(includeColumns);
    }

    public String getParser() {
        return parser == null ? "" : parser;
    }

    public void setParser(String parser) {
        this.parser = parser == null ? "" : parser;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    /** 预检要求跳过 DDL 发射（列数超限、未知列等）。 */
    public boolean isSkipEmit() {
        return skipEmit;
    }

    public void setSkipEmit(boolean skipEmit) {
        this.skipEmit = skipEmit;
    }
}
