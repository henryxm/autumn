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

package cn.org.autumn.table.mysql;

import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.ColumnInfo;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public class QuerySql {
    public static final String paramName = "paramName";
    public static final String createTable = "createTable";
    public static final String hasTable = "hasTable";
    public static final String getColumnMetas = "getColumnMetas";
    public static final String addColumns = "addColumns";
    public static final String modifyColumn = "modifyColumn";
    public static final String dropColumn = "dropColumn";
    public static final String dropPrimaryKey = "dropPrimaryKey";
    public static final String dropIndex = "dropIndex";
    public static final String dropTable = "dropTable";
    public static final String getTableMetas = "getTableMetas";
    public static final String getTableMetasWithMap = "getTableMetasWithMap";

    public String getColumnMetas() {
        return "select * from information_schema.columns where table_name = #{" + paramName + "} and table_schema = (select database()) order by ordinal_position";
    }

    public String getTableMetas(Map<String, Object> map) {
        String tableName = (String) map.get(paramName);
        int offset = 0;
        if (map.containsKey("offset"))
            offset = (int) map.get("offset");
        if (offset < 0)
            offset = 0;

        int rows = 0;
        if (map.containsKey("rows"))
            rows = (int) map.get("rows");
        if (rows <= 0)
            rows = Integer.MAX_VALUE;
        String whereClause = "";
        if (!StringUtils.isEmpty(tableName))
            whereClause = " table_name like concat('%', '" + tableName + "', '%') and";
        return "select * from information_schema.tables where" + whereClause + " table_schema = (select database()) order by create_time desc limit " + offset + ", " + rows;
    }

    public String hasTable() {
        return "select count(1) from information_schema.tables" +
                " where table_name = #{" + paramName + "} and table_schema = (select database())";
    }

    public String dropTable(Map<String, String> map) {
        String tableName = map.get(paramName);
        return "DROP TABLE IF EXISTS " + tableName;
    }

    private void appendCommon(ColumnInfo columnInfo, StringBuilder sb) {
        if (columnInfo.getTypeLength() == 0)
            sb.append("`" + columnInfo.getName() + "` " + columnInfo.getType());
        if (columnInfo.getTypeLength() == 1)
            sb.append("`" + columnInfo.getName() + "` " + columnInfo.getType() + "(" + columnInfo.getLength() + ")");
        if (columnInfo.getTypeLength() == 2) {
            sb.append("`" + columnInfo.getName() + "` ");
            sb.append(columnInfo.getType() + "(" + columnInfo.getLength() + ")," + columnInfo.getDecimalLength());
        }
        if (columnInfo.isNull())
            sb.append(" NULL");
        else
            sb.append(" NOT NULL");
        if (columnInfo.isAutoIncrement())
            sb.append(" AUTO_INCREMENT");
//        if (!columnInfo.isAutoIncrement()) {
//            if (!columnInfo.isNull() && !"NULL".equals(columnInfo.getDefaultValue())) {
//                sb.append(" DEFAULT " + columnInfo.getDefaultValue());
//            }
//        }
        if (!"NULL".equals(columnInfo.getDefaultValue())) {
            sb.append(" DEFAULT " + columnInfo.getDefaultValue());
        }
        if (!StringUtils.isEmpty(columnInfo.getComment()))
            sb.append(" COMMENT '" + columnInfo.getComment() + "'");
    }

    private String addColumnsInternal(final Map<String, Map<TableInfo, ColumnInfo>> map, String action) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(paramName);
        for (Map.Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            sb.append("ALTER TABLE `" + tableInfo.getName() + "` " + action + " ");
            ColumnInfo columnInfo = kv.getValue();
            appendCommon(columnInfo, sb);
            if (columnInfo.isKey())
                sb.append(" PRIMARY KEY");
            if (columnInfo.isUnique())
                sb.append(", UNIQUE KEY");
            sb.append(";");
        }
        return sb.toString();
    }


    public String dropColumn(final Map<String, Map<TableInfo, String>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, String> parameter = map.get(paramName);
        for (Map.Entry<TableInfo, String> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("ALTER TABLE `" + tableInfo.getName() + "`");
            String columnInfo = kv.getValue();
            stringBuilder.append(" DROP `" + columnInfo + "`");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }


    public String dropPrimaryKey(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(paramName);
        for (Map.Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("ALTER TABLE `" + tableInfo.getName() + "` MODIFY");
            ColumnInfo columnInfo = kv.getValue();
            appendCommon(columnInfo, stringBuilder);
            stringBuilder.append(", DROP PRIMARY KEY");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }

    public String dropIndex(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(paramName);
        for (Map.Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("ALTER TABLE `" + tableInfo.getName() + "` DROP INDEX");
            ColumnInfo columnInfo = kv.getValue();
            stringBuilder.append(" `" + columnInfo.getName() + "`");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }


    public String addColumns(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return addColumnsInternal(map, "ADD");
    }

    public String modifyColumn(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return addColumnsInternal(map, "MODIFY");
    }

    public String createTable(final Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, List<ColumnInfo>> parameter = map.get(paramName);
        for (Map.Entry<TableInfo, List<ColumnInfo>> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("CREATE TABLE `" + tableInfo.getName() + "`(");
            List<ColumnInfo> list = kv.getValue();
            for (ColumnInfo columnInfo : list) {
                appendCommon(columnInfo, stringBuilder);
                if (columnInfo.isKey())
                    stringBuilder.append(", PRIMARY KEY (`" + columnInfo.getName() + "`)");
                if (columnInfo.isUnique())
                    stringBuilder.append(", UNIQUE KEY (`" + columnInfo.getName() + "`)");
                if (list.indexOf(columnInfo) != list.size() - 1)
                    stringBuilder.append(", ");
            }
            stringBuilder.append(")");
            stringBuilder.append(" ENGINE=" + tableInfo.getEngine());
            stringBuilder.append(" DEFAULT CHARACTER SET " + tableInfo.getCharset());
            if (!StringUtils.isEmpty(tableInfo.getComment()))
                stringBuilder.append(" COMMENT='" + tableInfo.getComment() + "'");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }


}