package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.lang.reflect.Field;
import java.util.*;

public class TableInfo {

    private String name;
    private String prefix;
    private String comment;
    private String engine;
    private String charset;
    private Boolean hasBigDecimal;

    //表的主键
    private ColumnInfo pk;
    //表的列名(不包含主键)
    private List<ColumnInfo> columns;

    //类名(第一个字母大写)，如：sys_user => SysUser
    private String className;
    //类名(第一个字母小写)，如：sys_user => sysUser
    private String classname;

    private List<UniqueKeyInfo> uniqueKeyInfos;
    private List<IndexInfo> indexInfos;
    private List<IndexInfo> indexColumn;

    public TableInfo(Class<?> clazz) {
        initFrom(clazz);
    }

    public TableInfo(TableMeta tableMeta) {
        initFrom(tableMeta);
    }

    public void initFrom(TableMeta tableMeta) {
        setName(tableMeta.getTableName());
        setComment(tableMeta.getTableComment());
        setEngine(tableMeta.getEngine());
    }

    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名, 去掉表前缀
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix) && tableName.startsWith(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    public static TableInfo from(Class<?> clazz) {
        return new TableInfo(clazz);
    }

    public void initFrom(Class<?> clas) {
        Table table = clas.getAnnotation(Table.class);
        if (null == table) {
            return;
        }

        String tableName = table.value();
        if (StringUtils.isEmpty(tableName)) {
            String entityName = clas.getSimpleName();
            // remove for the end of entity
            if (entityName.endsWith("Entity")) {
                entityName = entityName.substring(0, entityName.length() - 6);
            }
            // Camel to underline
            entityName = HumpConvert.HumpToUnderline(entityName);

            if (!entityName.startsWith("_") && !StringUtils.isEmpty(table.prefix()) && !table.prefix().endsWith("_"))
                tableName = "_" + entityName;
            else
                tableName = entityName;
            if (StringUtils.isEmpty(table.prefix()) && tableName.startsWith("_"))
                tableName = tableName.substring(1);
            else
                tableName = table.prefix() + entityName;
        } else
            tableName = table.prefix() + table.value();

        this.uniqueKeyInfos = new ArrayList<>();
        UniqueKey uniqueKey = clas.getAnnotation(UniqueKey.class);
        if (null != uniqueKey) {
            uniqueKeyInfos.add(new UniqueKeyInfo(uniqueKey));
        }

        UniqueKeys uniqueKeys = clas.getAnnotation(UniqueKeys.class);

        if (null != uniqueKeys && uniqueKeys.value().length > 0) {
            for (UniqueKey u : uniqueKeys.value()) {
                uniqueKeyInfos.add(new UniqueKeyInfo(u));
            }
        }


        this.indexInfos = new ArrayList<>();
        Index index = clas.getAnnotation(Index.class);
        if (null != index) {
            indexInfos.add(new IndexInfo(index));
        }

        Indexes indexes = clas.getAnnotation(Indexes.class);

        if (null != indexes && indexes.value().length > 0) {
            for (Index u : indexes.value()) {
                indexInfos.add(new IndexInfo(u));
            }
        }

        Field[] fields = clas.getDeclaredFields();

        this.indexColumn = new ArrayList<>();
        for (Field field : fields) {
            Index k = field.getAnnotation(Index.class);
            if (null != k) {
                indexInfos.add(new IndexInfo(k, field));
            }

            Column column = field.getAnnotation(Column.class);
            if (null != column && column.isUnique()) {
                indexColumn.add(new IndexInfo(column, field));
            }
        }

        this.name = tableName;
        this.charset = table.charset();
        this.comment = table.comment();
        this.engine = table.engine();
        this.prefix = table.prefix();
    }

    public String buildUniqueSql() {
        StringBuilder stringBuilder = new StringBuilder();
        if (null != uniqueKeyInfos && uniqueKeyInfos.size() > 0) {
            Iterator<UniqueKeyInfo> uniqueKeyInfoIterator = uniqueKeyInfos.iterator();
            while (uniqueKeyInfoIterator.hasNext()) {
                stringBuilder.append("UNIQUE KEY ");
                UniqueKeyInfo uniqueKeyInfo = uniqueKeyInfoIterator.next();
                stringBuilder.append("`" + uniqueKeyInfo.getName() + "` (");
                Iterator<Map.Entry<String, Integer>> iterator = uniqueKeyInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("`" + kv.getKey() + "`(" + kv.getValue() + ")");
                    else
                        stringBuilder.append("`" + kv.getKey() + "`");
                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append(")");
                stringBuilder.append(" USING " + uniqueKeyInfo.getIndexMethod().toUpperCase());
                if (uniqueKeyInfoIterator.hasNext()) {
                    stringBuilder.append(", ");
                }
            }
        }
        return stringBuilder.toString();
    }

    public String buildIndexSql() {
        StringBuilder stringBuilder = new StringBuilder();
        if (null != indexInfos && indexInfos.size() > 0) {
            Iterator<IndexInfo> indexKeyInfoIterator = indexInfos.iterator();
            while (indexKeyInfoIterator.hasNext()) {
                IndexInfo indexInfo = indexKeyInfoIterator.next();
                if (!IndexTypeEnum.NORMAL.toString().equals(indexInfo.getIndexType()))
                    stringBuilder.append(indexInfo.getIndexType() + " ");
                stringBuilder.append("INDEX ");
                stringBuilder.append("`" + indexInfo.getName() + "` (");
                Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("`" + kv.getKey() + "`(" + kv.getValue() + ")");
                    else
                        stringBuilder.append("`" + kv.getKey() + "`");
                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append(")");
                stringBuilder.append(" USING " + indexInfo.getIndexMethod().toUpperCase());
                if (indexKeyInfoIterator.hasNext()) {
                    stringBuilder.append(", ");
                }
            }
        }
        return stringBuilder.toString();
    }

    public Collection<IndexInfo> merged() {
        Map<String, IndexInfo> _index = new HashMap<>();

        for (IndexInfo indexInfo : indexInfos) {
            if ("PRIMARY".equalsIgnoreCase(indexInfo.getKeyName()))
                continue;
            if (!_index.containsKey(indexInfo.getKeyName())) {
                _index.put(indexInfo.getKeyName(), indexInfo);
                if (!indexInfo.getFields().containsKey(indexInfo.getColumnName()))
                    indexInfo.getFields().put(indexInfo.getColumnName(), indexInfo.getSubPartInt());
            } else {
                IndexInfo t = _index.get(indexInfo.getKeyName());
                if (!t.getFields().containsKey(indexInfo.getColumnName()))
                    t.getFields().put(indexInfo.getColumnName(), indexInfo.getSubPartInt());
            }
        }
        return _index.values();
    }

    public String buildIndexKey() {
        StringBuilder stringBuilder = new StringBuilder();
        Collection<IndexInfo> merged = merged();
        if (null != merged && merged.size() > 0) {
            stringBuilder.append("@IndexKeys({");
            Iterator<IndexInfo> indexKeyInfoIterator = merged.iterator();
            while (indexKeyInfoIterator.hasNext()) {
                IndexInfo indexInfo = indexKeyInfoIterator.next();
                indexInfo.resolve();
                stringBuilder.append("@IndexKey(name = \"" + indexInfo.getName() + "\"" + ", indexType = IndexTypeEnum." + indexInfo.getIndexType() + ", indexMethod = IndexMethodEnum." + indexInfo.getIndexMethod() + ", fields = {");
                Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("@IndexKeyField(field = \"" + kv.getKey() + "\", length = " + kv.getValue() + ")");
                    else
                        stringBuilder.append("@IndexKeyField(field = \"" + kv.getKey() + "\")");
                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append("})");
                if (indexKeyInfoIterator.hasNext()) {
                    stringBuilder.append(",");
                }
            }
            stringBuilder.append("})");
        }
        return stringBuilder.toString();
    }

    public boolean isValid() {
        return StringUtils.isEmpty(name) ? false : true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setClassName(tableToJava(this.name, prefix));
        setClassname(StringUtils.uncapitalize(className));
    }

    public ColumnInfo getPk() {
        return pk;
    }

    public void setPk(ColumnInfo pk) {
        this.pk = pk;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        setName(name);
    }

    public List<UniqueKeyInfo> getUniqueKeyInfos() {
        return uniqueKeyInfos;
    }

    public void setUniqueKeyInfos(List<UniqueKeyInfo> uniqueKeyInfos) {
        this.uniqueKeyInfos = uniqueKeyInfos;
    }

    public List<IndexInfo> getIndexInfos() {
        return indexInfos;
    }

    public List<IndexInfo> getIndexInfosCombine() {
        if (null != uniqueKeyInfos && uniqueKeyInfos.size() > 0) {
            for (UniqueKeyInfo uniqueKeyInfo : uniqueKeyInfos) {
                indexInfos.add(IndexInfo.copy(uniqueKeyInfo));
            }
        }
        if (null != indexColumn && indexColumn.size() > 0) {
            for (IndexInfo indexInfo : indexColumn) {
                indexInfos.add(indexInfo);
            }
        }
        return indexInfos;
    }


    public void setIndexInfos(List<IndexInfo> indexInfos) {
        this.indexInfos = indexInfos;
    }

    public List<IndexInfo> getIndexColumn() {
        return indexColumn;
    }

    public void setIndexColumn(List<IndexInfo> indexColumn) {
        this.indexColumn = indexColumn;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Boolean getHasBigDecimal() {
        return hasBigDecimal;
    }

    public void setHasBigDecimal(Boolean hasBigDecimal) {
        this.hasBigDecimal = hasBigDecimal;
    }
}
