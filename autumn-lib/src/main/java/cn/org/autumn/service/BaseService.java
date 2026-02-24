package cn.org.autumn.service;

import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

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

    @Setter
    private String prefix = null;

    @Setter
    private String module = null;

    private final Map<Page<T>, Map<String, Object>> pageConditionStore = Collections.synchronizedMap(new WeakHashMap<>());

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
        return queryPage(_page, new QueryWrapper<>());
    }

    public PageUtils queryPage(Page<T> _page, QueryWrapper<T> entityEntityWrapper) {
        applyPageCondition(_page, entityEntityWrapper);
        Page<T> page = this.page(_page, entityEntityWrapper);
        return new PageUtils(page);
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
        bindPageCondition(_page, getCondition(params));
        return _page;
    }

    public Page<T> getPage(Map<String, Object> params, List<String> descs) {
        Page<T> _page = new Query<T>(params).getPage();
        bindPageCondition(_page, getCondition(params));
        if (null != descs && !descs.isEmpty()) {
            for (String desc : descs) {
                _page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc(desc));
            }
        }
        return _page;
    }

    protected void bindPageCondition(Page<T> page, Map<String, Object> condition) {
        if (page == null) {
            return;
        }
        if (condition == null || condition.isEmpty()) {
            pageConditionStore.remove(page);
            return;
        }
        pageConditionStore.put(page, new HashMap<>(condition));
    }

    protected void applyPageCondition(Page<T> page, QueryWrapper<T> wrapper) {
        if (page == null || wrapper == null) {
            return;
        }
        Map<String, Object> condition = pageConditionStore.remove(page);
        if (condition == null || condition.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : condition.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof String && StringUtils.isBlank((String) value)) {
                continue;
            }
            wrapper.eq(entry.getKey(), value);
        }
    }

    public Page<T> getPage(Map<String, Object> params, String... descs) {
        return getPage(params, new ArrayList<>(Arrays.asList(descs)));
    }

    public String getPrefix() {
        if (null == prefix) {
            Class<?> clazz = getModelClass();
            if (null != clazz) {
                Table table = clazz.getAnnotation(Table.class);
                String tmp = table.prefix();
                if (StringUtils.isBlank(tmp) && StringUtils.isNotBlank(table.value()) && table.value().contains("_")) {
                    tmp = table.value().split("_")[0];
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
                String tmp = table.module();
                if (StringUtils.isBlank(tmp) && StringUtils.isNotBlank(table.value()) && table.value().contains("_")) {
                    tmp = table.value().split("_")[0];
                }
                module = tmp;
            }
        }
        return module;
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
        //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
        return new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {tableComment, "modules/" + module + "/" + name, module + ":" + name + ":list," + module + ":" + name + ":info," + module + ":" + name + ":save," + module + ":" + name + ":update," + module + ":" + name + ":delete", "1", ico, order(), menu(), parentMenu(), module + "_" + name + "_table_comment"},
                {"查看", null, module + ":" + name + ":list," + module + ":" + name + ":info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, module + ":" + name + ":save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, module + ":" + name + ":update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, module + ":" + name + ":delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
    }
}