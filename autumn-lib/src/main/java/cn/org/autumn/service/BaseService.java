package cn.org.autumn.service;

import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.MybatisAbstractSQL;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.SqlPlus;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 基础服务类型
 *
 * @param <M> Mapper
 * @param <T> Entity
 */
@Slf4j
public abstract class BaseService<M extends BaseMapper<T>, T> extends ShareCacheService<M, T> implements BaseMenu {

    private String prefix = null;

    private String module = null;

    /**
     * 将查询条件过滤并转换为数据库中的字段条件进行查询
     *
     * @param params 查询条件，同时支持数据库的字段，属性字段(驼峰命名的对象数据)，和两者的混合条件
     * @return 检索的条件结果
     */
    public Map<String, Object> getCondition(Map<String, Object> params) {
        Map<String, Object> condition = new HashMap<>();
        Class<?> entity = getModelClass();
        if (null == entity)
            return condition;

        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (null == column)
                continue;
            String fieldName = field.getName();
            String columnName = HumpConvert.HumpToUnderline(fieldName);
            Object value = params.get(fieldName);
            if (null == value) {
                value = params.get(columnName);
            }
            if (null != value && !condition.containsKey(columnName)) {
                condition.put(columnName, value);
            }
        }
        return condition;
    }

    public PageUtils queryPage(Page<T> _page) {
        EntityWrapper<T> entityEntityWrapper = new EntityWrapper<>();
        return queryPage(_page, entityEntityWrapper);
    }

    public PageUtils queryPage(Page<T> _page, EntityWrapper<T> entityEntityWrapper) {
        if (entityEntityWrapper == null) {
            entityEntityWrapper = new EntityWrapper<>();
        }
        Page<T> page = this.selectPage(_page, entityEntityWrapper);
        // selectPage 内 SqlHelper.fillWrapper 会把 Page 排序写入 Wrapper，导致 COUNT 带 ORDER BY；
        // PostgreSQL 报错：column "…id" must appear in the GROUP BY or be used in an aggregate function
        page.setTotal(selectCountWithoutOrderBy(entityEntityWrapper));
        return new PageUtils(page);
    }

    /**
     * 统计条数：去掉 ORDER BY 再 count，避免 PostgreSQL 等对 {@code COUNT(...) ... ORDER BY} 报错。
     * <p>
     * <b>禁止</b>使用「新 {@link EntityWrapper} + 拼接 {@link EntityWrapper#getSqlSegment()}」：
     * 条件里的占位符形如 {@code #{ew.paramNameValuePairs.MPGENVALn}}，绑定在<b>原</b> {@link Wrapper} 私有
     * {@code paramNameValuePairs} 上；新 Wrapper 无参数会导致条件恒假、count 恒为 0。
     * <p>
     * 做法：反射临时清空 {@link MybatisAbstractSQL} 内部 {@code SQLCondition#orderBy}，在同一 {@code ew} 上
     * {@link BaseMapper#selectCount}，最后在 {@code finally} 中复原排序片段。
     * <p>
     * <b>与数据库类型的关系</b>：不在此拼接方言 SQL，只去掉条件构造器里的 ORDER 片段；最终 {@code COUNT} 仍由 MP + JDBC
     * 按当前数据源生成。对 MySQL、MariaDB、PostgreSQL、Oracle、SQL Server 等，语义均为「与原条件相同、无 {@code ORDER BY}
     * 的总行数统计」。最敏感的是 PostgreSQL 等对 {@code COUNT(*) ... ORDER BY} 校验较严的库，本路径正是为对齐其规则。
     * 若 {@link #tryDetachOrderByClauses} 反射失败，会回退为带 ORDER BY 的 {@code selectCount}，此类库仍可能报错（与是否「兼容库类型」无关，属回退路径限制）。
     * <p>
     * <b>版本风险</b>：依赖 MP 2.1.x 中 {@code Wrapper#sql} → {@code SqlPlus} → {@code SQLCondition#orderBy} 结构；
     * 升级 mybatis-plus 大版本或替换 Wrapper 实现时需回归本方法。
     */
    private int selectCountWithoutOrderBy(EntityWrapper<T> ew) {
        if (ew == null) {
            return baseMapper.selectCount(new EntityWrapper<>());
        }
        final List<String> orderBackup = tryDetachOrderByClauses(ew);
        try {
            return baseMapper.selectCount(ew);
        } catch (Exception ex) {
            log.error(
                    "selectCountWithoutOrderBy 失败，entity={}",
                    getModelClass() != null ? getModelClass().getName() : "?",
                    ex);
            throw ex;
        } finally {
            if (orderBackup != null) {
                restoreOrderByClauses(ew, orderBackup);
            }
        }
    }

    /**
     * @return 非 null 表示已成功卸下 ORDER BY，调用方必须在 finally 中 {@link #restoreOrderByClauses}；
     *         null 表示反射失败未改原 Wrapper（将带 ORDER BY 统计，部分库可能报错）
     */
    @SuppressWarnings("unchecked")
    private List<String> tryDetachOrderByClauses(EntityWrapper<T> ew) {
        try {
            Field sqlField = Wrapper.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            SqlPlus sqlPlus = (SqlPlus) sqlField.get(ew);
            Field innerSqlField = MybatisAbstractSQL.class.getDeclaredField("sql");
            innerSqlField.setAccessible(true);
            Object sqlCondition = innerSqlField.get(sqlPlus);
            Field orderByField = sqlCondition.getClass().getDeclaredField("orderBy");
            orderByField.setAccessible(true);
            List<String> orderBy = (List<String>) orderByField.get(sqlCondition);
            List<String> backup = new ArrayList<>(orderBy);
            orderBy.clear();
            return backup;
        } catch (ReflectiveOperationException e) {
            log.warn(
                    "selectCountWithoutOrderBy：无法反射清空 ORDER BY，将使用原 Wrapper 统计（PostgreSQL 等可能对 COUNT+ORDER BY 报错）：{}",
                    e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreOrderByClauses(EntityWrapper<T> ew, List<String> backup) {
        try {
            Field sqlField = Wrapper.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            SqlPlus sqlPlus = (SqlPlus) sqlField.get(ew);
            Field innerSqlField = MybatisAbstractSQL.class.getDeclaredField("sql");
            innerSqlField.setAccessible(true);
            Object sqlCondition = innerSqlField.get(sqlPlus);
            Field orderByField = sqlCondition.getClass().getDeclaredField("orderBy");
            orderByField.setAccessible(true);
            List<String> orderBy = (List<String>) orderByField.get(sqlCondition);
            orderBy.clear();
            orderBy.addAll(backup);
        } catch (ReflectiveOperationException e) {
            log.warn("selectCountWithoutOrderBy：恢复 ORDER BY 失败：{}", e.toString());
        }
    }

    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(getPage(params));
    }

    public PageUtils queryPage(Map<String, Object> params, List<String> descs) {
        return queryPage(getPage(params, descs));
    }

    public PageUtils queryPage(Map<String, Object> params, String... descs) {
        return queryPage(getPage(params, descs));
    }

    public Page<T> getPage(Map<String, Object> params) {
        Page<T> _page = new Query<T>(params).getPage();
        Map<String, Object> condition = getCondition(params);
        _page.setCondition(condition);
        return _page;
    }

    public Page<T> getPage(Map<String, Object> params, List<String> descs) {
        Page<T> _page = new Query<T>(params).getPage();
        Map<String, Object> condition = getCondition(params);
        _page.setCondition(condition);
        if (null != descs && !descs.isEmpty())
            _page.setDescs(descs);
        return _page;
    }

    public Page<T> getPage(Map<String, Object> params, String... descs) {
        return getPage(params, new ArrayList<>(Arrays.asList(descs)));
    }

    public String getPrefix() {
        if (null == prefix) {
            Class<?> clazz = getModelClass();
            if (null != clazz) {
                Table table = clazz.getAnnotation(Table.class);
                TableName tableName = clazz.getAnnotation(TableName.class);
                String tableValue = TableInfo.resolveTableValue(table, tableName);
                String tmp = table.prefix();
                if (StringUtils.isBlank(tmp) && StringUtils.isNotBlank(tableValue) && tableValue.contains("_")) {
                    tmp = tableValue.split("_")[0];
                }
                prefix = tmp;
            }
        }
        return prefix;
    }

    public String getModule() {
        if (null == module) {
            Class<?> clazz = getModelClass();
            if (null != clazz) {
                Table table = clazz.getAnnotation(Table.class);
                TableName tableName = clazz.getAnnotation(TableName.class);
                String tableValue = TableInfo.resolveTableValue(table, tableName);
                String tmp = table.module();
                if (StringUtils.isBlank(tmp) && StringUtils.isNotBlank(tableValue) && tableValue.contains("_")) {
                    tmp = tableValue.split("_")[0];
                }
                module = tmp;
            }
        }
        return module;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public static String toJava(String value) {
        return WordUtils.capitalizeFully(value, new char[]{'_'}).replace("_", "").trim();
    }

    public static String toLang(String value) {
        return WordUtils.capitalizeFully(value, new char[]{'_'}).replace("_", " ").trim();
    }

    public abstract String menu();

    public abstract String parentMenu();

    public String getMenu() {
        return menu();
    }

    public String button(String button) {
        return menu() + button;
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    public List<String[]> getMenuList() {
        return null;
    }

    public String[][] getMenuItems() {
        return null;
    }

    protected List<String[]> getLanguageItemsInternal() {
        String module = getModule();
        String prefix = getPrefix();
        Class<?> clazz = getModelClass();
        String name = clazz.getSimpleName().toLowerCase();
        String tableName = TableInfo.getTableName(clazz);
        Table table = clazz.getAnnotation(Table.class);
        if (null == table)
            return null;
        if (name.endsWith("entity")) {
            name = name.substring(0, name.length() - 6);
        }
        String lang = tableName.startsWith(prefix) ? tableName : tableName.replace(prefix + "_", "");
        List<String[]> items = new ArrayList<>();
        String tableComment = table.comment();
        if (StringUtils.isNotBlank(tableComment) && tableComment.contains(":")) {
            tableComment = tableComment.split(":")[0];
        }
        items.add(new String[]{module + "_" + name + "_table_comment", tableComment, toLang(lang)});
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (null == column)
                continue;
            String underline = HumpConvert.HumpToUnderline(field.getName());
            String columnComment = column.comment();
            if (StringUtils.isNotBlank(columnComment) && columnComment.contains(":")) {
                columnComment = columnComment.split(":")[0];
            }
            if (StringUtils.isBlank(columnComment)) {
                columnComment = HumpConvert.HumpToName(field.getName());
            }
            items.add(new String[]{module + "_" + name + "_column_" + underline, columnComment, toLang(underline)});
        }
        return items;
    }

    protected String[][] getMenuItemsInternal() {
        String module = getModule();
        Class<?> clazz = getModelClass();
        String name = clazz.getSimpleName().toLowerCase();
        Table table = clazz.getAnnotation(Table.class);
        if (null == table)
            return null;
        if (name.endsWith("entity")) {
            name = name.substring(0, name.length() - 6);
        }
        String tableComment = table.comment();
        if (StringUtils.isNotBlank(tableComment) && tableComment.contains(":")) {
            tableComment = tableComment.split(":")[0];
        }
        String ico = ico();
        if (!ico.startsWith("fa "))
            ico = "fa " + ico;
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {tableComment, "modules/" + module + "/" + name, module + ":" + name + ":list," + module + ":" + name + ":info," + module + ":" + name + ":save," + module + ":" + name + ":update," + module + ":" + name + ":delete", "1", ico, order(), menu(), parentMenu(), module + "_" + name + "_table_comment"},
                {"查看", null, module + ":" + name + ":list," + module + ":" + name + ":info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, module + ":" + name + ":save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, module + ":" + name + ":update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, module + ":" + name + ":delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}