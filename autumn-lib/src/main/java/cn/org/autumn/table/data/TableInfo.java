package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.table.utils.HumpConvert;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.lang.reflect.Field;
import java.util.*;

@Getter
@Setter
public class TableInfo {
    private String name;
    private String prefix;
    private String module;
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
    //类名(全字母小写)，如：sys_user => sysuser
    private String filename;

    //英语名
    private String enLang;

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
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "").trim();
    }

    public static String columnToLang(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", " ").trim();
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

    public static String tableToLang(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix) && tableName.startsWith(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToLang(tableName);
    }

    public static TableInfo from(Class<?> clazz) {
        return new TableInfo(clazz);
    }

    public static String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        TableName tableName = clazz.getAnnotation(TableName.class);
        if (null == table) {
            return "";
        }
        String name = table.value();
        String prefix = table.prefix();
        if (StringUtils.isBlank(name) && null != tableName)
            name = tableName.value();
        if (StringUtils.isEmpty(name)) {
            String entityName = clazz.getSimpleName();
            // remove for the end of entity
            if (entityName.endsWith("Entity")) {
                entityName = entityName.substring(0, entityName.length() - 6);
            }
            // Camel to underline
            entityName = HumpConvert.HumpToUnderline(entityName);
            if (!entityName.startsWith("_") && !StringUtils.isEmpty(prefix) && !prefix.endsWith("_"))
                name = "_" + entityName;
            else
                name = entityName;
            if (StringUtils.isEmpty(prefix) && name.startsWith("_"))
                name = name.substring(1);
            else {
                if (entityName.startsWith(prefix))
                    name = entityName;
                else
                    name = prefix + entityName;
            }
        } else {
            if (table.value().startsWith(prefix))
                name = table.value();
            else
                name = prefix + table.value();
        }
        return name;
    }

    public void initFrom(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (null == table)
            return;
        String name = getTableName(clazz);
        if (StringUtils.isBlank(name))
            return;
        this.uniqueKeyInfos = new ArrayList<>();
        UniqueKey uniqueKey = clazz.getAnnotation(UniqueKey.class);
        if (null != uniqueKey) {
            uniqueKeyInfos.add(new UniqueKeyInfo(uniqueKey));
        }
        UniqueKeys uniqueKeys = clazz.getAnnotation(UniqueKeys.class);
        if (null != uniqueKeys && uniqueKeys.value().length > 0) {
            for (UniqueKey u : uniqueKeys.value()) {
                uniqueKeyInfos.add(new UniqueKeyInfo(u));
            }
        }
        this.indexInfos = new ArrayList<>();
        Index index = clazz.getAnnotation(Index.class);
        if (null != index) {
            indexInfos.add(new IndexInfo(index));
        }
        Indexes indexes = clazz.getAnnotation(Indexes.class);
        if (null != indexes && indexes.value().length > 0) {
            for (Index u : indexes.value()) {
                indexInfos.add(new IndexInfo(u));
            }
        }
        Field[] fields = clazz.getDeclaredFields();
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
        this.name = name;
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
                stringBuilder.append("`").append(uniqueKeyInfo.getName()).append("` (");
                Iterator<Map.Entry<String, Integer>> iterator = uniqueKeyInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("`").append(kv.getKey()).append("`(").append(kv.getValue()).append(")");
                    else
                        stringBuilder.append("`").append(kv.getKey()).append("`");
                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append(")");
                stringBuilder.append(" USING ").append(uniqueKeyInfo.getIndexMethod().toUpperCase());
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
                    stringBuilder.append(indexInfo.getIndexType()).append(" ");
                stringBuilder.append("INDEX ");
                stringBuilder.append("`").append(indexInfo.getName()).append("` (");
                Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("`").append(kv.getKey()).append("`(").append(kv.getValue()).append(")");
                    else
                        stringBuilder.append("`").append(kv.getKey()).append("`");
                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append(")");
                stringBuilder.append(" USING ").append(indexInfo.getIndexMethod().toUpperCase());
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
            stringBuilder.append("@Indexes({");
            Iterator<IndexInfo> indexKeyInfoIterator = merged.iterator();
            while (indexKeyInfoIterator.hasNext()) {
                IndexInfo indexInfo = indexKeyInfoIterator.next();
                indexInfo.resolve();
                stringBuilder.append("@Index(name = \"").append(indexInfo.getName()).append("\"").append(", indexType = IndexTypeEnum.").append(indexInfo.getIndexType()).append(", indexMethod = IndexMethodEnum.").append(indexInfo.getIndexMethod()).append(", fields = {");
                Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("@IndexField(field = \"").append(kv.getKey()).append("\", length = ").append(kv.getValue()).append(")");
                    else
                        stringBuilder.append("@IndexField(field = \"").append(kv.getKey()).append("\")");
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

    public boolean isValid() {
        return !StringUtils.isEmpty(name);
    }

    public void setName(String name) {
        this.name = name;
        setClassName(tableToJava(this.name, prefix));
        setEnLang(tableToLang(this.name, prefix));
        setClassname(StringUtils.uncapitalize(className));
        setFilename(className.toLowerCase());
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        setName(name);
    }

    public List<IndexInfo> getIndexInfosCombine() {
        if (null != uniqueKeyInfos && uniqueKeyInfos.size() > 0) {
            for (UniqueKeyInfo uniqueKeyInfo : uniqueKeyInfos) {
                indexInfos.add(IndexInfo.copy(uniqueKeyInfo));
            }
        }
        if (null != indexColumn && indexColumn.size() > 0) {
            indexInfos.addAll(indexColumn);
        }
        return indexInfos;
    }
}
