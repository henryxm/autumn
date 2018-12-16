/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.table.dao;

import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.QuerySql;
import cn.org.autumn.table.mysql.TableMeta;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static cn.org.autumn.table.mysql.QuerySql.*;

@Mapper
@Repository
public interface TableDao {

    /**
     * 创建表
     *
     * @param map
     */
    @SelectProvider(type = QuerySql.class, method = createTable)
    void createTable(@Param(paramName) Map<TableInfo, List<Object>> map);

    /**
     * 根据表名查询表在库中是否存在
     * @param tableName
     * @return true 表存在  false 表不存在
     */
    @SelectProvider(type = QuerySql.class, method = hasTable)
    @ResultType(Boolean.class)
    boolean hasTable(@Param(paramName) String tableName);

    /**
     * 查询表的字段
     *
     * @param tableName
     * @return
     */
    @SelectProvider(type = QuerySql.class, method = getColumnMetas)
    @ResultType(List.class)
    List<ColumnMeta> getColumnMetas(@Param(paramName) String tableName);

    /**
     * get table meta data
     *
     * support empty parameter and one parameter with "like" searching.
     * @param tableName
     * @return
     */
    @SelectProvider(type = QuerySql.class, method = getTableMetas)
    @ResultType(List.class)
    List<TableMeta> getTableMetas(@Param(paramName) String tableName,
                                  @Param("offset") int offset,
                                  @Param("rows") int rows);

    /**
     *
     * @param tableName
     * @return
     */
    @SelectProvider(type = QuerySql.class, method = getTableMetas)
    @ResultType(List.class)
    List<TableMeta> getTableMetas(@Param(paramName) String tableName);

    @SelectProvider(type = QuerySql.class, method = showKeys)
    @ResultType(List.class)
    List<UniqueKeyInfo> getTableKeys(@Param(paramName) String tableName);

    @SelectProvider(type = QuerySql.class, method = getTableCount)
    @ResultType(Integer.class)
    Integer getTableCount();
    /**
     * 为表增加字段
     *
     * @param map
     */
    @SelectProvider(type = QuerySql.class, method = addColumns)
    void addColumns(@Param(paramName) Map<TableInfo, Object> map);

    /**
     * 修改表字段
     *
     * @param map
     */
    @SelectProvider(type = QuerySql.class, method = modifyColumn)
    void modifyColumn(@Param(paramName) Map<TableInfo, Object> map);

    /**
     * 删除表字段
     *
     * @param map
     */
    @SelectProvider(type = QuerySql.class, method = dropColumn)
    void dropColumn(@Param(paramName) Map<TableInfo, Object> map);

    /**
     * 删除主键
     * @param map
     */
    @SelectProvider(type = QuerySql.class, method = dropPrimaryKey)
    void dropPrimaryKey(@Param(paramName) Map<TableInfo, Object> map);

    /**
     * 删除索引
     * @param map
     */
    @SelectProvider(type = QuerySql.class, method = dropIndex)
    void dropIndex(@Param(paramName) Map<TableInfo, Object> map);

    /**
     * 如果表存在，就删除
     * @param tableName
     */
    @SelectProvider(type = QuerySql.class, method = dropTable)
    void dropTable(@Param(paramName) String tableName);

}
