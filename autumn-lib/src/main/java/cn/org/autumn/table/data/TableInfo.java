package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang.StringUtils;

public class TableInfo {

    private String name;
    private String prefix;
    private String comment;
    private String engine;
    private String charset;

    public TableInfo(Class<?> clazz) {
        initFrom(clazz);
    }

    public static TableInfo from(Class<?> clazz){
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

        this.name = tableName;
        this.charset = table.charset();
        this.comment = table.comment();
        this.engine = table.engine();
        this.prefix = table.prefix();

    }

    public boolean isValid() {
        return StringUtils.isEmpty(name) ? false : true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
}
