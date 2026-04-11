package cn.org.autumn.table.dao.postgresql;

import cn.org.autumn.table.relational.dialect.postgresql.PostgresRelationalSchemaSql;
import cn.org.autumn.table.relational.model.ColumnMeta;
import cn.org.autumn.table.relational.model.TableMeta;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

import static cn.org.autumn.table.relational.dialect.postgresql.PostgresRelationalSchemaSql.*;

/**
 * PostgreSQL 元数据查询（DDL 由 {@link PostgresRelationalTableOperations} 通过脚本执行）。
 */
@Mapper
@Repository
public interface PostgresTableDao {

    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = hasTable)
    @ResultType(Integer.class)
    Integer countTable(@Param(paramName) String tableName);

    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = getColumnMetas)
    List<ColumnMeta> getColumnMetas(@Param(paramName) String tableName);

    /** 分页表元数据；勿与 {@link #getTableMetas(String)} 重载同名，否则 MyBatis statement id 冲突。 */
    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = getTableMetas)
    List<TableMeta> getTableMetasPage(@Param(paramName) String tableName,
                                      @Param("offset") int offset,
                                      @Param("rows") int rows);

    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = getTableMetas)
    List<TableMeta> getTableMetas(@Param(paramName) String tableName);

    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = showKeys)
    List<UniqueKeyInfo> getTableKeys(@Param(paramName) String tableName);

    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = showIndex)
    List<IndexInfo> getTableIndex(@Param(paramName) String tableName);

    @SelectProvider(type = PostgresRelationalSchemaSql.class, method = getTableCount)
    Integer getTableCount();
}
