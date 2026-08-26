package cn.org.autumn.modules.db.service;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.modules.db.model.TableScanColumnMeta;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.utils.HumpConvert;
import java.lang.reflect.Field;
import java.sql.Clob;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

/**
 * 预览结果单元格格式化。
 */
final class TableScanResultFormatter {

    private TableScanResultFormatter() {
    }

    static Object format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return formatDate(new Date(((Timestamp) value).getTime()));
        }
        if (value instanceof java.sql.Date) {
            return value.toString();
        }
        if (value instanceof Date) {
            return formatDate((Date) value);
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (bytes.length <= 64) {
                return Base64.getEncoder().encodeToString(bytes);
            }
            return "[binary:" + bytes.length + " bytes]";
        }
        if (value instanceof Clob) {
            try {
                Clob clob = (Clob) value;
                long len = clob.length();
                if (len > 4096) {
                    return clob.getSubString(1, 4096) + "...";
                }
                return clob.getSubString(1, (int) len);
            } catch (Exception e) {
                return "[clob]";
            }
        }
        return value;
    }

    private static String formatDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        return format.format(date);
    }
}
