package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.*;

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
        if (this == o) return true;
        if (!(o instanceof IndexInfo)) return false;
        IndexInfo indexInfo = (IndexInfo) o;
        Map<String, Integer> f = indexInfo.getFields();
        List<String> dd = new ArrayList<>();
        for (Map.Entry<String, Integer> k : f.entrySet()) {
            dd.add(k.getKey().replace("_", "").toLowerCase().trim());
        }

        if (f.size() != getFields().size())
            return false;
        for (String s : getFields().keySet()) {
            if (!dd.contains(s.replace("_", "").toLowerCase().trim()))
                return false;
        }

        return getName().replace("_", "").equalsIgnoreCase(indexInfo.getName().replace("_", "")) &&
                getIndexType().equals(indexInfo.getIndexType()) &&
                getIndexMethod().equals(indexInfo.getIndexMethod());
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
                fields.put(indexKeyField.field(), indexKeyField.length());
            }
        }
    }

    public IndexInfo(Index index, Field field) {
        fields = new HashMap<>();
        this.name = index.name();
        if (name.isEmpty() && null != field) {
            this.name = field.getName();
            fields.put(field.getName(), 0);
        }
        this.indexMethod = index.indexMethod().toString();
        this.indexType = index.indexType().toString();
        this.comment = index.comment();
        if (index.fields().length > 0) {
            for (IndexField indexKeyField : index.fields()) {
                fields.put(indexKeyField.field(), indexKeyField.length());
            }
        }
    }

    public IndexInfo(Column index, Field field) {
        fields = new HashMap<>();
        this.indexMethod = "BTREE";
        this.indexType = "UNIQUE";
        this.comment = index.comment();
        this.name = field.getName();
        fields.put(field.getName(), index.length());
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
