package cn.org.autumn.table.relational.model;

/**
 * 当前库单表字符集（批量预载用），字段名对齐 MyBatis 映射。
 */
public class TableCharsetMeta {

    private String tableName;
    private String characterSetName;

    public TableCharsetMeta() {
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getCharacterSetName() {
        return characterSetName;
    }

    public void setCharacterSetName(String characterSetName) {
        this.characterSetName = characterSetName;
    }
}
