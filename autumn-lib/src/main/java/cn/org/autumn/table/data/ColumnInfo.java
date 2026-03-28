package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.UniqueKey;
import cn.org.autumn.table.annotation.UniqueKeyFields;
import cn.org.autumn.table.annotation.UniqueKeys;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

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


    private boolean hasUniqueKey;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 英语
     */
    private String enLang;

    //属性名称(第一个字母大写)，如：user_name => UserName
    private String attrName;
    //属性名称(第一个字母小写)，如：user_name => userName
    private String attrname;
    //属性类型 列的数据类型，转换成Java类型
    private String attrType;
    //auto_increment
    private String extra;

    private String genAnnotation;

    /**
     * 是否为枚举字段
     */
    private boolean enumType;

    /**
     * 枚举字段可用的常量集合（仅在 enumType=true 时有效）
     */
    private Set<String> enumConstants;


    public ColumnInfo(Field field, UniqueKeys uniqueKeys, UniqueKey uniqueKey) {
        initFrom(field, uniqueKeys, uniqueKey);
    }

    public ColumnInfo(ColumnMeta field) {
        initFrom(field);
    }

    public static ColumnInfo from(Field field, UniqueKeys uniqueKeys, UniqueKey uniqueKey) {
        return new ColumnInfo(field, uniqueKeys, uniqueKey);
    }

    public void initFrom(ColumnMeta column) {
        setName(column.getColumnName());
        setType(column.getDataType());
        setComment(column.getColumnComment());
        setExtra(column.getExtra());
        if ("PRI".equalsIgnoreCase(column.getColumnKey())) {
            setKey(true);
        }
        setNull(column.getNullable());
        setDefaultValue(column.getColumnDefault());
        String ct = column.getColumnType();
        String[] tt = ct.split("\\(");
        if (tt.length > 1) {
            String len = tt[1].split("\\)")[0];
            if (len.contains(",")) {
                String[] rr = len.split(",");
                int length = Integer.parseInt(rr[0]);
                setLength(length);
                length = Integer.parseInt(rr[1]);
                setDecimalLength(length);
            } else {
                int length = Integer.parseInt(len);
                setLength(length);
            }
        } else
            length = 0;
        setGenAnnotation(buildAnnotation());
    }

    private String buildAnnotation() {
        StringBuilder sb = new StringBuilder();
        sb.append("@Column(");
        String divider = "";
        if (this.isKey) {
            sb.append("isKey = true");
            divider = ", ";
        }
        if (!"varchar".equalsIgnoreCase(type)) {
            sb.append(divider + "type = \"" + type + "\"");
            divider = ", ";
        }
        if (length != 255 && length != 0) {
            sb.append(divider + "length = " + length);
            divider = ", ";
        }
        if (!isNull) {
            sb.append(divider + "isNull = false");
            divider = ", ";
        }
        if (isAutoIncrement) {
            sb.append(divider + "isAutoIncrement = true");
            divider = ", ";
        }
        if (decimalLength > 0) {
            sb.append(divider + "decimalLength = " + decimalLength);
            divider = ", ";
        }
        if (null != defaultValue && !"NULL".equalsIgnoreCase(defaultValue)) {
            sb.append(divider + "defaultValue = \"" + defaultValue + "\"");
            divider = ", ";
        }

        if (!StringUtils.isEmpty(comment)) {
            sb.append(divider + "comment = \"" + comment + "\"");
        }

        sb.append(")");
        return sb.toString();
    }

    public String getGenAnnotation() {
        return genAnnotation;
    }

    public void setGenAnnotation(String genAnnotation) {
        this.genAnnotation = genAnnotation;
    }

    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    public static String columnToEnLang(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", " ");
    }

    public void initFrom(Field field, UniqueKeys uniqueKeys, UniqueKey uniqueKey) {
        Column column = field.getAnnotation(Column.class);
        if (null == column) {
            return;
        }
        String columnName = column.value();
        if (StringUtils.isEmpty(columnName)) {
            columnName = field.getName();
            columnName = HumpConvert.HumpToUnderline(columnName);
        }
        this.name = columnName;
        this.type = column.type().toLowerCase();
        this.length = column.length();
        this.decimalLength = column.decimalLength();

        Class<?> ft = field.getType();
        if (boolean.class.equals(ft) || Boolean.class.equals(ft)) {
            this.type = DataType.TINYINT;
            this.length = 1;
        } else if (Date.class.equals(ft) || java.sql.Date.class.equals(ft)) {
            this.type = DataType.DATETIME;
        } else if (long.class.equals(ft) || Long.class.equals(ft)) {
            this.type = DataType.BIGINT;
            this.length = ColumnLengthCorrection.normalizeBigInt(column.length());
        } else if (int.class.equals(ft) || Integer.class.equals(ft)) {
            this.type = DataType.INT;
            this.length = ColumnLengthCorrection.normalizeInt(column.length());
        } else if (short.class.equals(ft) || Short.class.equals(ft)) {
            this.type = DataType.SMALLINT;
            this.length = ColumnLengthCorrection.normalizeSmallInt(column.length());
        } else if (byte.class.equals(ft) || Byte.class.equals(ft)) {
            this.type = DataType.TINYINT;
            this.length = ColumnLengthCorrection.normalizeTinyInt(column.length());
        } else if (float.class.equals(ft) || Float.class.equals(ft)) {
            this.type = DataType.FLOAT;
            this.length = ColumnLengthCorrection.normalizeFloat(column.length());
            if (this.decimalLength < 0 || this.decimalLength > 8) {
                this.decimalLength = 2;
            }
        } else if (double.class.equals(ft) || Double.class.equals(ft)) {
            this.type = DataType.DOUBLE;
            this.length = ColumnLengthCorrection.normalizeDouble(column.length());
            if (this.decimalLength < 0 || this.decimalLength > 15) {
                this.decimalLength = 2;
            }
        } else if (BigDecimal.class.equals(ft)) {
            this.type = DataType.DECIMAL;
            if (this.decimalLength < 0 || this.decimalLength > 28) {
                this.decimalLength = 2;
            }
            this.length = ColumnLengthCorrection.normalizeDecimalM(column.length(), this.decimalLength);
        }
        if (this.decimalLength < 0) {
            this.decimalLength = 0;
        }
        // 主键或唯一键时设置必须不为null
        if (column.isKey() || column.isUnique())
            this.isNull = false;
        else
            this.isNull = column.isNull();
        this.isKey = column.isKey();
        this.isAutoIncrement = column.isAutoIncrement();
        this.defaultValue = column.defaultValue();
        this.isUnique = column.isUnique();
        this.hasUniqueKey = isUnique;
        if (null != uniqueKey) {
            UniqueKeyFields[] uniqueKeyFields = uniqueKey.fields();
            for (UniqueKeyFields u : uniqueKeyFields) {
                if (name.equalsIgnoreCase(u.field())) {
                    hasUniqueKey = true;
                    break;
                }
            }
        }
        if (!hasUniqueKey && null != uniqueKeys) {
            UniqueKey[] uu = uniqueKeys.value();
            for (UniqueKey u1 : uu) {
                UniqueKeyFields[] uniqueKeyFields = u1.fields();
                for (UniqueKeyFields u : uniqueKeyFields) {
                    if (name.equalsIgnoreCase(u.field())) {
                        hasUniqueKey = true;
                        break;
                    }
                }
            }
        }
        this.comment = column.comment();
        if (StringUtils.isBlank(this.comment)) {
            this.comment = HumpConvert.HumpToName(field.getName());
        }

        if (field.getType().isEnum()) {
            this.enumType = true;
            this.enumConstants = new LinkedHashSet<>();
            Object[] constants = field.getType().getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    if (constant instanceof Enum<?>) {
                        this.enumConstants.add(((Enum<?>) constant).name());
                    }
                }
            }
        }
    }


    public boolean isValid() {
        return !StringUtils.isEmpty(this.name);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        if (null != comment)
            this.comment = comment.trim();
    }

    public String getEnLang() {
        return enLang;
    }

    public void setEnLang(String enLang) {
        this.enLang = enLang;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setAttrName(columnToJava(name));
        setEnLang(columnToEnLang(name));
        setAttrname((name.startsWith("_") ? "_" : "") + StringUtils.uncapitalize(attrName));
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

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getAttrname() {
        return attrname;
    }

    public void setAttrname(String attrname) {
        this.attrname = attrname;
    }

    public String getAttrType() {
        return attrType;
    }

    public void setAttrType(String attrType) {
        this.attrType = attrType;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
        if ("auto_increment".equalsIgnoreCase(this.extra)) {
            this.isAutoIncrement = true;
        }
    }

    public boolean hasUniqueKey() {
        return hasUniqueKey;
    }

    public boolean isEnumType() {
        return enumType;
    }

    public Set<String> getEnumConstants() {
        return enumConstants;
    }
}
