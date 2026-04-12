package cn.org.autumn.config;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 解析 SQLite TEXT 列中的日期时间（与 {@link SqliteDateTypeHandler}、JDBC 拦截器共用）。
 */
public final class SqliteDateTextParseUtil {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter[] IN_LOCAL = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
    };

    private SqliteDateTextParseUtil() {
    }

    /**
     * @param s 非 null 时按文本解析；空串返回 null
     */
    public static Date parseToDate(String s) throws SQLException {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        Date parsed = tryParseAsInstantOrLocalDateTime(t);
        if (parsed != null) {
            return parsed;
        }
        parsed = tryLegacyPatterns(t);
        if (parsed != null) {
            return parsed;
        }
        try {
            long n = Long.parseLong(t);
            return n > 10_000_000_000L ? new Date(n) : new Date(n * 1000L);
        } catch (NumberFormatException ignored) {
            // fall through
        }
        throw new SQLException("Unparseable SQLite date/time text: " + t);
    }

    private static Date tryParseAsInstantOrLocalDateTime(String t) {
        try {
            return Date.from(Instant.parse(t));
        } catch (DateTimeParseException ignored) {
            // continue
        }
        for (DateTimeFormatter f : IN_LOCAL) {
            try {
                if (f == DateTimeFormatter.ISO_LOCAL_DATE) {
                    LocalDate d = LocalDate.parse(t, f);
                    return Date.from(d.atStartOfDay(SYSTEM_ZONE).toInstant());
                }
                LocalDateTime ldt = LocalDateTime.parse(t, f);
                return Date.from(ldt.atZone(SYSTEM_ZONE).toInstant());
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static Date tryLegacyPatterns(String t) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd",
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.ROOT);
                sdf.setTimeZone(TimeZone.getDefault());
                return sdf.parse(t);
            } catch (ParseException ignored) {
                // next
            }
        }
        return null;
    }

    static boolean isTextualTimestampParseFailure(SQLException e) {
        if (e == null || e.getMessage() == null) {
            return false;
        }
        String m = e.getMessage().toLowerCase(Locale.ROOT);
        return m.contains("parsing time stamp")
                || m.contains("parse timestamp")
                || m.contains("error parsing time");
    }
}
