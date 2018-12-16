package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.annotation.UniqueKey;
import cn.org.autumn.table.annotation.UniqueKeyFields;
import cn.org.autumn.table.annotation.UniqueKeys;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

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
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "");
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

    public Collection<UniqueKeyInfo> merged() {
        Map<String, UniqueKeyInfo> _unique = new HashMap<>();

        for (UniqueKeyInfo uniqueKeyInfo : uniqueKeyInfos) {
            if ("PRIMARY".equalsIgnoreCase(uniqueKeyInfo.getKeyName()))
                continue;
            if (!_unique.containsKey(uniqueKeyInfo.getKeyName())) {
                _unique.put(uniqueKeyInfo.getKeyName(), uniqueKeyInfo);
                if (!uniqueKeyInfo.getFields().containsKey(uniqueKeyInfo.getColumnName()))
                    uniqueKeyInfo.getFields().put(uniqueKeyInfo.getColumnName(), uniqueKeyInfo.getSubPartInt());
            } else {
                UniqueKeyInfo t = _unique.get(uniqueKeyInfo.getKeyName());
                if (!t.getFields().containsKey(uniqueKeyInfo.getColumnName()))
                    t.getFields().put(uniqueKeyInfo.getColumnName(), uniqueKeyInfo.getSubPartInt());
            }
        }
        return _unique.values();
    }

    public String buildUniqueKey() {
        StringBuilder stringBuilder = new StringBuilder();
        Collection<UniqueKeyInfo> merged = merged();
        if (null != merged && merged.size() > 0) {
            stringBuilder.append("@UniqueKeys({");
            Iterator<UniqueKeyInfo> uniqueKeyInfoIterator = merged.iterator();
            while (uniqueKeyInfoIterator.hasNext()) {
                UniqueKeyInfo uniqueKeyInfo = uniqueKeyInfoIterator.next();
                stringBuilder.append("@UniqueKey(name = \"" + uniqueKeyInfo.getName() + "\"" + ", fields = {");
                Iterator<Map.Entry<String, Integer>> iterator = uniqueKeyInfo.getFields().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> kv = iterator.next();
                    if (kv.getValue() > 0)
                        stringBuilder.append("@UniqueKeyFields(field = \"" + kv.getKey() + "\", length = " + kv.getValue() + ")");
                    else
                        stringBuilder.append("@UniqueKeyFields(field = \"" + kv.getKey() + "\")");
                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append("})");
                if (uniqueKeyInfoIterator.hasNext()) {
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
