package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.UniqueKey;
import cn.org.autumn.table.annotation.UniqueKeyFields;

import java.util.HashMap;
import java.util.Map;

public class UniqueKeyInfo {
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

    public UniqueKeyInfo() {
        fields = new HashMap<>();
    }

    public UniqueKeyInfo(UniqueKey uniqueKey) {
        this.name = uniqueKey.name();
        this.indexMethod = uniqueKey.indexMethod().toString();
        this.indexType = uniqueKey.indexType().toString();
        this.comment = uniqueKey.comment();
        fields = new HashMap<>();
        if (uniqueKey.fields().length > 0) {
            for (UniqueKeyFields uniqueKeyFields : uniqueKey.fields()) {
                fields.put(uniqueKeyFields.field(), uniqueKeyFields.length());
            }
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

    public int getSubPartInt(){
        if(null != subPart){
            try{
                return Integer.valueOf(subPart);
            }catch (Exception e){

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
