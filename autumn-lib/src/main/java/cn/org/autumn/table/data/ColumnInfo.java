package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;

/**
 * 用于存放创建表的字段信息
 */
public class ColumnInfo {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 类型长度
     */
    private int length;

    /**
     * 类型小数长度
     */
    private int decimalLength;

    /**
     * 字段是否非空
     */
    private boolean isNull;

    /**
     * 字段是否是主键
     */
    private boolean isKey;

    /**
     * 主键是否自增
     */
    private boolean isAutoIncrement;

    /**
     * 字段默认值
     */
    private String defaultValue;

    /**
     * 该类型需要几个长度（例如，需要小数位数的，那么总长度和小数长度就是2个长度）一版只有0、1、2三个可选值，自动从配置的类型中获取的
     */
    private int typeLength;

    /**
     * 值是否唯一
     */
    private boolean isUnique;

    /**
     * 字段注释
     */
    private String comment;


    public ColumnInfo(Field field) {
        initFrom(field);
    }

    public static ColumnInfo from(Field field) {
        return new ColumnInfo(field);
    }

    public void initFrom(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (null == column) {
            return;
        }
        String columnName = column.value();
        if (StringUtils.isEmpty(columnName)) {
            columnName = field.getName();
            columnName = HumpConvert.HumpToUnderline(columnName);
            if(columnName.startsWith("_"))
                columnName = columnName.substring(1);
        }
        this.name = columnName;
        this.type = column.type().toLowerCase();
        this.length = column.length();
        this.decimalLength = column.decimalLength();
        // 主键或唯一键时设置必须不为null
        if (column.isKey() || column.isUnique())
            this.isNull = false;
        else
            this.isNull = column.isNull();
        this.isKey = column.isKey();
        this.isAutoIncrement = column.isAutoIncrement();
        this.defaultValue = column.defaultValue();
        this.isUnique = column.isUnique();
        this.comment = column.comment();
    }

    public boolean isValid() {
        return StringUtils.isEmpty(this.name) ? false : true;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getDecimalLength() {
        return decimalLength;
    }

    public void setDecimalLength(int decimalLength) {
        this.decimalLength = decimalLength;
    }

    public boolean isNull() {
        return isNull;
    }

    public void setNull(boolean aNull) {
        this.isNull = aNull;
    }

    public boolean isKey() {
        return isKey;
    }

    public void setKey(boolean key) {
        this.isKey = key;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.isAutoIncrement = autoIncrement;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getTypeLength() {
        return typeLength;
    }

    public void setTypeLength(int typeLength) {
        this.typeLength = typeLength;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        this.isUnique = unique;
    }

}
