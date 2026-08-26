package cn.org.autumn.modules.db.model;

import java.util.Collections;
import java.util.Set;
import lombok.Getter;

/**
 * 实体索引列的类型元数据，用于扫描时的参数绑定与结果格式化。
 */
@Getter
public class TableScanColumnMeta {

    private final String sqlType;
    private final Class<?> fieldType;
    private final boolean enumType;
    private final Set<String> enumConstants;
    private final boolean fieldEncrypted;

    public TableScanColumnMeta(String sqlType, Class<?> fieldType, boolean enumType, Set<String> enumConstants, boolean fieldEncrypted) {
        this.sqlType = sqlType == null ? "" : sqlType.toLowerCase();
        this.fieldType = fieldType == null ? String.class : fieldType;
        this.enumType = enumType;
        this.enumConstants = enumConstants == null ? Collections.emptySet() : enumConstants;
        this.fieldEncrypted = fieldEncrypted;
    }

    public static TableScanColumnMeta stringDefault() {
        return new TableScanColumnMeta("varchar", String.class, false, Collections.emptySet(), false);
    }

    public String displayType() {
        if (enumType) {
            return "enum";
        }
        if (fieldEncrypted) {
            return sqlType + " (encrypted)";
        }
        String java = fieldType.getSimpleName();
        if (String.class.equals(fieldType)) {
            return sqlType.isEmpty() ? "string" : sqlType;
        }
        return sqlType + " / " + java;
    }
}
