package cn.org.autumn.modules.db.service;

import cn.org.autumn.modules.db.model.TableScanColumnMeta;
import cn.org.autumn.table.data.DataType;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * 将用户输入的特征值按实体列类型解析并绑定到 JDBC 参数。
 */
final class TableScanValueBinder {

    private TableScanValueBinder() {
    }

    static BindOutcome bind(PreparedStatement ps, int parameterIndex, String rawValue, TableScanColumnMeta meta) throws Exception {
        CoercedValue coerced = coerce(rawValue, meta);
        if (!coerced.isOk()) {
            return BindOutcome.skip(coerced.getSkipReason());
        }
        apply(ps, parameterIndex, coerced);
        return BindOutcome.ok(coerced.getBindKind());
    }

    static BindOutcome canBind(String rawValue, TableScanColumnMeta meta) {
        CoercedValue coerced = coerce(rawValue, meta);
        if (!coerced.isOk()) {
            return BindOutcome.skip(coerced.getSkipReason());
        }
        return BindOutcome.ok(coerced.getBindKind());
    }

    private static CoercedValue coerce(String rawValue, TableScanColumnMeta meta) {
        if (meta == null) {
            meta = TableScanColumnMeta.stringDefault();
        }
        if (meta.isFieldEncrypted()) {
            return CoercedValue.skip("列已启用存储加密，无法以明文特征值扫描");
        }
        if (StringUtils.isBlank(rawValue)) {
            return CoercedValue.skip("特征值为空");
        }
        String value = rawValue.trim();
        Class<?> fieldType = meta.getFieldType();
        String sqlType = meta.getSqlType();
        if (meta.isEnumType()) {
            return coerceEnum(value, meta.getEnumConstants());
        }
        if (isBooleanField(fieldType)) {
            return coerceBoolean(value);
        }
        if (isIntegralField(fieldType, sqlType)) {
            return coerceIntegral(value, fieldType, sqlType);
        }
        if (isDecimalField(fieldType, sqlType)) {
            return coerceDecimal(value, fieldType);
        }
        if (isTemporalField(fieldType, sqlType)) {
            return coerceTemporal(value);
        }
        return CoercedValue.of("string", value);
    }

    private static void apply(PreparedStatement ps, int parameterIndex, CoercedValue coerced) throws Exception {
        Object val = coerced.getValue();
        switch (coerced.getBindKind()) {
            case "long":
                ps.setLong(parameterIndex, ((Number) val).longValue());
                break;
            case "int":
            case "tinyint":
            case "boolean":
                ps.setInt(parameterIndex, ((Number) val).intValue());
                break;
            case "short":
                ps.setShort(parameterIndex, ((Number) val).shortValue());
                break;
            case "byte":
                ps.setByte(parameterIndex, ((Number) val).byteValue());
                break;
            case "float":
                ps.setFloat(parameterIndex, ((Number) val).floatValue());
                break;
            case "double":
                ps.setDouble(parameterIndex, ((Number) val).doubleValue());
                break;
            case "decimal":
                ps.setBigDecimal(parameterIndex, (BigDecimal) val);
                break;
            case "datetime":
                ps.setTimestamp(parameterIndex, (Timestamp) val);
                break;
            default:
                ps.setString(parameterIndex, String.valueOf(val));
                break;
        }
    }

    private static CoercedValue coerceEnum(String value, Set<String> constants) {
        if (constants != null && !constants.isEmpty() && !constants.contains(value)) {
            return CoercedValue.skip("枚举值不在允许范围内: " + value);
        }
        return CoercedValue.of("enum", value);
    }

    private static CoercedValue coerceIntegral(String value, Class<?> fieldType, String sqlType) {
        try {
            if (Long.class.equals(fieldType) || long.class.equals(fieldType) || DataType.BIGINT.equals(sqlType)) {
                return CoercedValue.of("long", Long.parseLong(value));
            }
            if (Integer.class.equals(fieldType) || int.class.equals(fieldType) || DataType.INT.equals(sqlType)) {
                return CoercedValue.of("int", Integer.parseInt(value));
            }
            if (Short.class.equals(fieldType) || short.class.equals(fieldType) || DataType.SMALLINT.equals(sqlType)) {
                return CoercedValue.of("short", Short.parseShort(value));
            }
            if (Byte.class.equals(fieldType) || byte.class.equals(fieldType)) {
                return CoercedValue.of("byte", Byte.parseByte(value));
            }
            if (DataType.TINYINT.equals(sqlType)) {
                return CoercedValue.of("tinyint", Integer.parseInt(value));
            }
            return CoercedValue.of("long", Long.parseLong(value));
        } catch (NumberFormatException e) {
            return CoercedValue.skip("数值格式与列类型不匹配: " + displayIntegralType(fieldType, sqlType));
        }
    }

    private static CoercedValue coerceDecimal(String value, Class<?> fieldType) {
        try {
            if (Float.class.equals(fieldType) || float.class.equals(fieldType)) {
                return CoercedValue.of("float", Float.parseFloat(value));
            }
            if (Double.class.equals(fieldType) || double.class.equals(fieldType)) {
                return CoercedValue.of("double", Double.parseDouble(value));
            }
            return CoercedValue.of("decimal", new BigDecimal(value));
        } catch (NumberFormatException e) {
            return CoercedValue.skip("小数格式与列类型不匹配");
        }
    }

    private static CoercedValue coerceBoolean(String value) {
        Boolean parsed = parseBoolean(value);
        if (parsed == null) {
            return CoercedValue.skip("布尔值格式无效，请使用 true/false 或 1/0");
        }
        return CoercedValue.of("boolean", parsed ? 1 : 0);
    }

    private static CoercedValue coerceTemporal(String value) {
        Timestamp timestamp = parseTimestamp(value);
        if (timestamp == null) {
            return CoercedValue.skip("日期时间格式无效，支持 yyyy-MM-dd HH:mm:ss 或毫秒时间戳");
        }
        return CoercedValue.of("datetime", timestamp);
    }

    static Timestamp parseTimestamp(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String text = value.trim();
        if (text.matches("^-?\\d+$")) {
            try {
                long epoch = Long.parseLong(text);
                if (text.length() <= 10) {
                    epoch *= 1000L;
                }
                return new Timestamp(epoch);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern, java.util.Locale.ROOT);
                format.setLenient(false);
                java.util.Date parsed = format.parse(text);
                return new Timestamp(parsed.getTime());
            } catch (java.text.ParseException ignored) {
                // try next
            }
        }
        return null;
    }

    static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value) || "no".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private static boolean isIntegralField(Class<?> fieldType, String sqlType) {
        if (Long.class.equals(fieldType) || long.class.equals(fieldType)
                || Integer.class.equals(fieldType) || int.class.equals(fieldType)
                || Short.class.equals(fieldType) || short.class.equals(fieldType)
                || Byte.class.equals(fieldType) || byte.class.equals(fieldType)) {
            return true;
        }
        return DataType.BIGINT.equals(sqlType) || DataType.INT.equals(sqlType)
                || DataType.SMALLINT.equals(sqlType) || DataType.TINYINT.equals(sqlType);
    }

    private static boolean isDecimalField(Class<?> fieldType, String sqlType) {
        if (BigDecimal.class.equals(fieldType) || Float.class.equals(fieldType) || float.class.equals(fieldType)
                || Double.class.equals(fieldType) || double.class.equals(fieldType)) {
            return true;
        }
        return DataType.DECIMAL.equals(sqlType) || DataType.FLOAT.equals(sqlType) || DataType.DOUBLE.equals(sqlType);
    }

    private static boolean isBooleanField(Class<?> fieldType) {
        return boolean.class.equals(fieldType) || Boolean.class.equals(fieldType);
    }

    private static boolean isTemporalField(Class<?> fieldType, String sqlType) {
        if (java.util.Date.class.equals(fieldType) || java.sql.Date.class.equals(fieldType) || Timestamp.class.equals(fieldType)) {
            return true;
        }
        return DataType.DATETIME.equals(sqlType) || DataType.DATE.equals(sqlType) || DataType.TIMESTAMP.equals(sqlType);
    }

    private static String displayIntegralType(Class<?> fieldType, String sqlType) {
        if (fieldType != null && !fieldType.equals(String.class)) {
            return fieldType.getSimpleName();
        }
        return sqlType;
    }

    static final class BindOutcome {
        private final boolean bound;
        private final String bindKind;
        private final String skipReason;

        private BindOutcome(boolean bound, String bindKind, String skipReason) {
            this.bound = bound;
            this.bindKind = bindKind;
            this.skipReason = skipReason;
        }

        static BindOutcome ok(String bindKind) {
            return new BindOutcome(true, bindKind, null);
        }

        static BindOutcome skip(String reason) {
            return new BindOutcome(false, null, reason);
        }

        boolean isBound() {
            return bound;
        }

        String getBindKind() {
            return bindKind;
        }

        String getSkipReason() {
            return skipReason;
        }
    }

    private static final class CoercedValue {
        private final boolean ok;
        private final String bindKind;
        private final Object value;
        private final String skipReason;

        private CoercedValue(boolean ok, String bindKind, Object value, String skipReason) {
            this.ok = ok;
            this.bindKind = bindKind;
            this.value = value;
            this.skipReason = skipReason;
        }

        static CoercedValue of(String bindKind, Object value) {
            return new CoercedValue(true, bindKind, value, null);
        }

        static CoercedValue skip(String reason) {
            return new CoercedValue(false, null, null, reason);
        }

        boolean isOk() {
            return ok;
        }

        String getBindKind() {
            return bindKind;
        }

        Object getValue() {
            return value;
        }

        String getSkipReason() {
            return skipReason;
        }
    }
}
