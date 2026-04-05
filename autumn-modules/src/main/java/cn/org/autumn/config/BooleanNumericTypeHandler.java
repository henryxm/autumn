package cn.org.autumn.config;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java {@code boolean} 与库中的「布尔语义」互转；MyBatis 泛型仍用 {@link Boolean} 装箱。
 * <ul>
 *   <li><b>PostgreSQL</b>：列多为原生 {@code boolean}，使用 {@link PreparedStatement#setBoolean(int, boolean)}，
 *   避免 {@code setInt} 与 boolean 列冲突。</li>
 *   <li><b>MySQL 等</b>：列多为 {@code tinyint/smallint/integer} 存 0/1，使用 {@link PreparedStatement#setInt(int, int)}，
 *   避免 JDBC boolean 与整型列冲突。</li>
 * </ul>
 * 同一 PostgreSQL 库若混用 {@code boolean} 与整型开关列，无法仅靠本类区分，请统一 DDL（或升级 MyBatis-Plus 做字段级 {@code typeHandler}）。
 * 须与 {@link MybatisPlusConfig}、{@link BooleanNumericParameterInterceptor} 配合。
 * <p>
 * {@link AutumnDatabaseHolder} 为 null 时（如单测）按非 PG 处理，使用整型绑定。
 */
public class BooleanNumericTypeHandler extends BaseTypeHandler<Boolean> {

    private final AutumnDatabaseHolder databaseHolder;

    public BooleanNumericTypeHandler() {
        this(null);
    }

    public BooleanNumericTypeHandler(AutumnDatabaseHolder databaseHolder) {
        this.databaseHolder = databaseHolder;
    }

    private boolean bindAsPostgresqlBoolean() {
        if (databaseHolder == null) {
            return false;
        }
        return databaseHolder.getType() == AutumnDatabaseType.POSTGRESQL;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        if (bindAsPostgresqlBoolean()) {
            if (parameter == null) {
                ps.setBoolean(i, false);
            } else {
                ps.setBoolean(i, parameter);
            }
            return;
        }
        if (parameter == null) {
            ps.setInt(i, 0);
        } else {
            setNonNullParameter(ps, i, parameter, jdbcType);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        if (bindAsPostgresqlBoolean()) {
            ps.setBoolean(i, parameter);
        } else {
            ps.setInt(i, parameter ? 1 : 0);
        }
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toBooleanPrimitive(rs.getObject(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toBooleanPrimitive(rs.getObject(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toBooleanPrimitive(cs.getObject(columnIndex));
    }

    private static boolean toBooleanPrimitive(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        }
        if (o instanceof String) {
            String s = ((String) o).trim();
            if (s.isEmpty()) {
                return false;
            }
            if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
                return true;
            }
            if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
                return false;
            }
        }
        return false;
    }
}
