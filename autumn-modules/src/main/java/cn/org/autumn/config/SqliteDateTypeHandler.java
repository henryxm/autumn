package cn.org.autumn.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * SQLite 将 {@code DATETIME}/{@code TIMESTAMP} 映射为 {@code TEXT}（见 {@code JdbcDdlColumnTypes#sqlite}），
 * xerial 驱动对 TEXT 列调用 {@link ResultSet#getTimestamp(String)} 常报 {@code Error parsing time stamp}。
 * 本类用 {@link ResultSet#getString(int)} 取值并解析；写入时用 {@code yyyy-MM-dd HH:mm:ss} 文本与库内格式一致。
 * <p>
 * 若 MyBatis-Plus 2.x 映射未使用该 TypeHandler，仍由 {@link SqliteJdbcResultAccessInterceptor} 在 JDBC 层兜底。
 */
@MappedTypes(Date.class)
@MappedJdbcTypes({JdbcType.TIMESTAMP, JdbcType.DATE, JdbcType.TIME})
public class SqliteDateTypeHandler extends BaseTypeHandler<Date> {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType) throws SQLException {
        LocalDateTime ldt = LocalDateTime.ofInstant(parameter.toInstant(), SYSTEM_ZONE);
        ps.setString(i, OUT.format(ldt));
    }

    @Override
    public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseColumn(rs.getString(columnName), rs.wasNull());
    }

    @Override
    public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseColumn(rs.getString(columnIndex), rs.wasNull());
    }

    @Override
    public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseColumn(cs.getString(columnIndex), cs.wasNull());
    }

    private static Date parseColumn(String s, boolean wasNull) throws SQLException {
        if (wasNull || s == null) {
            return null;
        }
        return SqliteDateTextParseUtil.parseToDate(s);
    }
}
