package cn.org.autumn.crypto;

import cn.org.autumn.annotation.FieldEncrypt;
import lombok.Getter;

import java.lang.reflect.Field;

/**
 * {@link cn.org.autumn.annotation.FieldEncrypt} 字段元数据。
 * <p>
 * {@code vector} 仅用于加密列 IV；{@code hashField} 为盲索引 Java 字段（须实体手写 {@code @Column}，不自动 DDL）。
 */
@Getter
public class FieldMeta {

    private final Field field;
    private final String fieldName;
    private final String vector;
    private final boolean searchable;
    private final String hashFieldName;
    private final Field hashField;

    public FieldMeta(Field field, FieldEncrypt annotation, Field hashField) {
        this.field = field;
        this.fieldName = field.getName();
        this.vector = annotation.vector();
        this.searchable = annotation.searchable();
        if (annotation.hashField().isEmpty()) {
            this.hashFieldName = fieldName + "Hash";
        } else {
            this.hashFieldName = annotation.hashField();
        }
        this.hashField = hashField;
        this.field.setAccessible(true);
        if (this.hashField != null) {
            this.hashField.setAccessible(true);
        }
    }

    public boolean hasHashField() {
        return hashField != null;
    }
}
