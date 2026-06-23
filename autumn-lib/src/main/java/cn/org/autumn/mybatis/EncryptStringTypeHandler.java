package cn.org.autumn.mybatis;

import cn.org.autumn.crypto.FieldEncryptService;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 可选 String 字段 TypeHandler；显式 {@code @TableField(typeHandler = EncryptStringTypeHandler.class)} 时启用。
 * <p>
 * 默认路径为 {@link cn.org.autumn.base.EncryptModuleService} + {@link FieldEncryptService}，一般无需 TypeHandler。
 */
public class EncryptStringTypeHandler extends BaseTypeHandler<String> {

    private final FieldEncryptService fieldEncryptService;

    public EncryptStringTypeHandler(FieldEncryptService fieldEncryptService) {
        this.fieldEncryptService = fieldEncryptService;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, fieldEncryptService.encryptValue(parameter, ""));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fieldEncryptService.decryptValue(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fieldEncryptService.decryptValue(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fieldEncryptService.decryptValue(cs.getString(columnIndex));
    }
}
