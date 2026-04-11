package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.utils.HumpConvert;

import java.lang.reflect.Field;
import java.util.*;

public class IndexInfo {
    String name;
    Map<String, Integer> fields;
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

    public IndexInfo() {
        fields = new HashMap<>();
    }

    public void resolve() {
        if ("0".equals(nonUnique)) {
            indexType = IndexTypeEnum.UNIQUE.name();
        } else {
            if ("FULLTEXT".equalsIgnoreCase(indexType)) {
                indexType = IndexTypeEnum.FULLTEXT.name();
            } else
                indexType = IndexTypeEnum.NORMAL.name();
        }
        if (null == indexMethod) {
            indexMethod = IndexMethodEnum.BTREE.name();
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
     * Derby / DB2 / PostgreSQL / SQLite 等：索引名在 schema（或库）内全局唯一；注解默认名为列名（如 {@code tag}）会在多表冲突。
     * DDL 使用 {@code 表名_逻辑名} 落库后，JDBC 读回物理名，与注解 {@link #getName()} 需用本方法比对。
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
        if (a.equalsIgnoreCase(b)) {
            return true;
        }
        String prefix = tableName + "_";
        return a.equalsIgnoreCase(prefix + b) || b.equalsIgnoreCase(prefix + a);
    }

    /** 与 {@link #equals} 相同的列集合与索引类型/方法约束，不比较名称。 */
    public boolean sameIndexColumnsAndKind(IndexInfo that) {
        if (that == null) {
            return false;
        }
        Map<String, Integer> fThis = this.getFields();
        Map<String, Integer> fThat = that.getFields();
        if (fThis == null) {
            fThis = Collections.emptyMap();
        }
        if (fThat == null) {
            fThat = Collections.emptyMap();
        }
        if (fThis.size() != fThat.size()) {
            return false;
        }
        Set<String> normThat = new HashSet<>();
        for (Map.Entry<String, Integer> k : fThat.entrySet()) {
            normThat.add(normalizeIndexFieldKey(k.getKey()));
        }
        for (String s : fThis.keySet()) {
            if (!normThat.contains(normalizeIndexFieldKey(s))) {
                return false;
            }
        }
        return Objects.equals(this.getIndexType(), that.getIndexType())
                && Objects.equals(this.getIndexMethod(), that.getIndexMethod());
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
        String aa = a == null ? "" : a.replace("_", "");
        String bb = b == null ? "" : b.replace("_", "");
        return aa.equalsIgnoreCase(bb);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getFields(), getIndexType(), getIndexMethod(), getComment());
    }

    public IndexInfo(Index indexKey) {
        this.name = indexKey.name();
        this.indexMethod = indexKey.indexMethod().toString();
        this.indexType = indexKey.indexType().toString();
        this.comment = indexKey.comment();
        fields = new HashMap<>();
        if (indexKey.fields().length > 0) {
            for (IndexField indexKeyField : indexKey.fields()) {
                String n = HumpConvert.HumpToUnderline(indexKeyField.field());
                fields.put(n, indexKeyField.length());
            }
        }
    }

    public IndexInfo(Index index, Field field) {
        fields = new HashMap<>();
        this.name = index.name();
        if (name.isEmpty() && null != field) {
            name = HumpConvert.HumpToUnderline(field.getName());
            Column column = field.getAnnotation(Column.class);
            int rawLen = column != null ? column.length() : 0;
            fields.put(name, IndexPrefixRules.effectivePrefixLength(field, column, rawLen));
        }
        this.indexMethod = index.indexMethod().toString();
        this.indexType = index.indexType().toString();
        this.comment = index.comment();
        if (index.fields().length > 0) {
            for (IndexField indexKeyField : index.fields()) {
                String n = HumpConvert.HumpToUnderline(indexKeyField.field());
                fields.put(n, indexKeyField.length());
            }
        }
    }

    public IndexInfo(Column columnAnn, Field field) {
        fields = new HashMap<>();
        this.indexMethod = "BTREE";
        this.indexType = "UNIQUE";
        this.comment = columnAnn.comment();
        this.name = HumpConvert.HumpToUnderline(field.getName());
        fields.put(name, IndexPrefixRules.effectivePrefixLength(field, columnAnn, columnAnn.length()));
    }

    /**
     * 类级 {@link Index}（无字段上下文）中显式的 {@link cn.org.autumn.table.annotation.IndexField#length()} 可能误用于数值列，此处按实体字段类型与 {@link Column#type()} 收敛。
     */
    public void applyPrefixLengthPolicy(Class<?> entityClass) {
        IndexPrefixRules.applyPrefixLengthPolicy(fields, entityClass);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.keyName = name;
        this.name = name;
    }

    public Map<String, Integer> getFields() {
        return fields;
    }

    public void setFields(Map<String, Integer> fields) {
        this.fields = fields;
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
        this.nonUnique = nonUnique;
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
}
