package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.LengthCount;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataType {

    @LengthCount
    public static final String INT = "int";

    @LengthCount
    public static final String VARCHAR = "varchar";

    @LengthCount(LengthCount = 0)
    public static final String TEXT = "text";

    @LengthCount(LengthCount = 0)
    public static final String MEDIUMTEXT = "mediumtext";

    @LengthCount(LengthCount = 0)
    public static final String LONGTEXT = "longtext";

    @LengthCount(LengthCount = 0)
    public static final String DATETIME = "datetime";

    @LengthCount(LengthCount = 2)
    public static final String DECIMAL = "decimal";

    @LengthCount(LengthCount = 2)
    public static final String DOUBLE = "double";

    @LengthCount(LengthCount = 2)
    public static final String FLOAT = "float";

    @LengthCount
    public static final String CHAR = "char";

    @LengthCount(LengthCount = 0)
    public static final String TINYBLOB = "tinyblob";

    @LengthCount(LengthCount = 0)
    public static final String TINYTEXT = "tinytext";

    @LengthCount(LengthCount = 0)
    public static final String BLOB = "blob";

    @LengthCount(LengthCount = 0)
    public static final String MEDIUMBLOB = "mediumblob";

    @LengthCount(LengthCount = 0)
    public static final String LONGBLOB = "longblob";

    @LengthCount(LengthCount = 1)
    public static final String BIGINT = "bigint";

    @LengthCount(LengthCount = 1)
    public static final String TINYINT = "tinyint";

    @LengthCount(LengthCount = 1)
    public static final String SMALLINT = "smallint";

    @LengthCount(LengthCount = 0)
    public static final String DATE = "date";

    @LengthCount(LengthCount = 0)
    public static final String TIME = "time";

    @LengthCount(LengthCount = 0)
    public static final String YEAR = "year";

    @LengthCount(LengthCount = 0)
    public static final String TIMESTAMP = "timestamp";

    @LengthCount(LengthCount = 0)
    public static final String ENUM = "enum";

    @LengthCount(LengthCount = 0)
    public static final String SET = "set";

    public static final ArrayList<String> intArray= new ArrayList<>();
    static {
        intArray.add(INT);
        intArray.add(BIGINT);
        intArray.add(DECIMAL);
        intArray.add(DOUBLE);
        intArray.add(FLOAT);
        intArray.add(TINYINT);
        intArray.add(SMALLINT);
    }
}
