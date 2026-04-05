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

    private static final int MAX_PAGE_CONDITION_STORE_SIZE = 20;

    /**
     * 使用 WeakHashMap 避免“已失去引用但未执行查询”的 Page 持续占用内存。
     * 同时配合上限淘汰，防止极端场景下短时间大量堆积。
     */
    private final Map<Page<T>, PageConditionHolder> pageConditionStore = Collections.synchronizedMap(new WeakHashMap<>());

    private static class PageConditionHolder {
        private final Map<String, Object> condition;
        private final long bindAt;

        private PageConditionHolder(Map<String, Object> condition) {
            this.condition = condition;
            this.bindAt = System.nanoTime();
        }
    }

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
            Object value = normalizeParamValue(params.get(fieldName));
            if (null == value) {
                value = normalizeParamValue(params.get(columnName));
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
        if (entityEntityWrapper == null) {
            entityEntityWrapper = new QueryWrapper<>();
        }
        _page.setSearchCount(false);
        Page<T> page = this.page(_page, entityEntityWrapper);
        page.setTotal(selectCountWithoutOrderBy(entityEntityWrapper));
        return new PageUtils(page);
    }

    public Page<T> page(Page<T> _page, QueryWrapper<T> entityEntityWrapper) {
        applyPageCondition(_page, entityEntityWrapper);
        return super.page(_page, entityEntityWrapper);
    }

    /**
     * 分页 COUNT 时去掉 ORDER BY，避免 PostgreSQL 等对 COUNT 带 ORDER BY 报错。
     */
    protected long selectCountWithoutOrderBy(QueryWrapper<T> ew) {
        if (ew == null) {
            return baseMapper.selectCount(new QueryWrapper<>());
        }
        String seg = ew.getSqlSegment();
        if (StringUtils.isBlank(seg)) {
            return baseMapper.selectCount(new QueryWrapper<>());
        }
        String s = seg.trim();
        int ob = s.toUpperCase(Locale.ROOT).lastIndexOf("ORDER BY");
        if (ob >= 0) {
            s = s.substring(0, ob).trim();
        }
        if (StringUtils.isBlank(s)) {
            return baseMapper.selectCount(new QueryWrapper<>());
        }
        if (s.toUpperCase(Locale.ROOT).startsWith("WHERE ")) {
            s = s.substring(6).trim();
        }
        if (StringUtils.isBlank(s)) {
            return baseMapper.selectCount(new QueryWrapper<>());
        }
        QueryWrapper<T> countEw = new QueryWrapper<>();
        countEw.apply(s);
        return baseMapper.selectCount(countEw);
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
        Map<String, Object> normalized = normalizeQueryParams(params);
        Page<T> _page = new Query<T>(normalized).getPage();
        bindPageCondition(_page, getCondition(normalized));
        return _page;
    }

    public Page<T> getPage(Map<String, Object> params, List<String> descs) {
        Map<String, Object> normalized = normalizeQueryParams(params);
        Page<T> _page = new Query<T>(normalized).getPage();
        bindPageCondition(_page, getCondition(normalized));
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
        pageConditionStore.put(page, new PageConditionHolder(new HashMap<>(condition)));
        evictPageConditionStoreIfNeeded();
    }

    protected void applyPageCondition(Page<T> page, QueryWrapper<T> wrapper) {
        if (page == null || wrapper == null) {
            return;
        }
        PageConditionHolder holder = pageConditionStore.remove(page);
        Map<String, Object> condition = holder == null ? null : holder.condition;
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

    private void evictPageConditionStoreIfNeeded() {
        synchronized (pageConditionStore) {
            int overflow = pageConditionStore.size() - MAX_PAGE_CONDITION_STORE_SIZE;
            while (overflow > 0 && !pageConditionStore.isEmpty()) {
                Page<T> oldestPage = null;
                long oldestBindAt = Long.MAX_VALUE;
                for (Map.Entry<Page<T>, PageConditionHolder> entry : pageConditionStore.entrySet()) {
                    PageConditionHolder holder = entry.getValue();
                    if (holder != null && holder.bindAt < oldestBindAt) {
                        oldestBindAt = holder.bindAt;
                        oldestPage = entry.getKey();
                    }
                }
                if (oldestPage == null) {
                    break;
                }
                pageConditionStore.remove(oldestPage);
                overflow--;
            }
        }
    }

    public Page<T> getPage(Map<String, Object> params, String... descs) {
        return getPage(params, new ArrayList<>(Arrays.asList(descs)));
    }

    /**
     * 统一兼容 Spring/Servlet 在不同版本下的参数绑定差异：
     * 1) String / String[] / Collection 统一取首值
     * 2) rows -> limit, sord -> order
     * 3) Query 依赖的分页排序参数强制转为 String
     */
    protected Map<String, Object> normalizeQueryParams(Map<String, Object> params) {
        Map<String, Object> normalized = new HashMap<>();
        if (params == null || params.isEmpty()) {
            return normalized;
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Object value = normalizeParamValue(entry.getValue());
            if (value == null) {
                continue;
            }
            normalized.put(key, value);
        }
        // jqGrid 兼容参数
        if (!normalized.containsKey("limit") && normalized.containsKey("rows")) {
            normalized.put("limit", normalized.get("rows"));
        }
        if (!normalized.containsKey("order") && normalized.containsKey("sord")) {
            normalized.put("order", normalized.get("sord"));
        }
        normalizeToString(normalized, "page");
        normalizeToString(normalized, "limit");
        normalizeToString(normalized, "rows");
        normalizeToString(normalized, "sidx");
        normalizeToString(normalized, "order");
        normalizeToString(normalized, "sord");
        return normalized;
    }

    protected Object normalizeParamValue(Object value) {
        if (value == null) {
            return null;
        }
        Object normalized = value;
        if (normalized instanceof String[]) {
            String[] values = (String[]) normalized;
            normalized = values.length > 0 ? values[0] : null;
        } else if (normalized instanceof Object[]) {
            Object[] values = (Object[]) normalized;
            normalized = values.length > 0 ? values[0] : null;
        } else if (normalized instanceof Collection) {
            Collection<?> values = (Collection<?>) normalized;
            normalized = values.isEmpty() ? null : values.iterator().next();
        }
        if (normalized instanceof String) {
            String text = ((String) normalized).trim();
            return StringUtils.isBlank(text) ? null : text;
        }
        return normalized;
    }

    protected void normalizeToString(Map<String, Object> params, String key) {
        if (!params.containsKey(key) || params.get(key) == null) {
            return;
        }
        Object value = params.get(key);
        if (!(value instanceof String)) {
            params.put(key, String.valueOf(value));
        }
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