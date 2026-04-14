package cn.org.autumn.service;

import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
     * 分页 COUNT 时去掉 ORDER BY（及分页插件写入的 {@code LIMIT/OFFSET}），避免 PostgreSQL 等对 {@code COUNT} 带排序/分页尾句报错。
     * <p>
     * 勿对 {@code getSqlSegment()} 截断后再 {@code new QueryWrapper().apply(s)}：片段里的 {@code #{ew.paramNameValuePairs.xxx}}
     * 仍指向原序列化名，新 Wrapper 无对应参数会导致条件失效、count 恒为 0。
     * 做法：{@link QueryWrapper#clone()} 后仅在副本上 {@link MergeSegments#getOrderBy()}{@code .clear()}，并失效 {@link MergeSegments}
     * 的片段缓存（否则仍返回含 ORDER BY 的旧 {@code sqlSegment}），再清空副本的 {@code lastSql}（常见为 {@code LIMIT}）。
     */
    protected long selectCountWithoutOrderBy(QueryWrapper<T> ew) {
        if (ew == null) {
            return baseMapper.selectCount(new QueryWrapper<>());
        }
        final QueryWrapper<T> countEw;
        try {
            countEw = ew.clone();
        } catch (Exception ex) {
            log.warn(
                    "selectCountWithoutOrderBy：QueryWrapper.clone 失败，改为在原 Wrapper 上临时去掉 ORDER BY 后统计，entity={}，cause={}",
                    getModelClass() != null ? getModelClass().getName() : "?",
                    ex.toString());
            return selectCountWithOrderByStrippedTemporarily(ew);
        }
        try {
            MergeSegments exp = countEw.getExpression();
            if (exp != null) {
                exp.getOrderBy().clear();
                invalidateMergeSegmentsSqlCache(exp);
            }
            clearQueryWrapperLastSql(countEw);
            return baseMapper.selectCount(countEw);
        } catch (Exception ex) {
            log.warn(
                    "selectCountWithoutOrderBy：去排序后 selectCount 仍失败，改为在原 Wrapper 上临时去掉 ORDER BY 后统计，entity={}，cause={}",
                    getModelClass() != null ? getModelClass().getName() : "?",
                    ex.toString());
            return selectCountWithOrderByStrippedTemporarily(ew);
        }
    }

    /**
     * 在已执行完分页列表查询后，临时清空 ORDER BY 做 COUNT，再还原，避免 Derby 等对 {@code COUNT(*) ... ORDER BY} 报错。
     * <p>
     * 典型调用链中 {@code ew} 在 {@link #queryPage(Page, QueryWrapper)} 内于 {@link #page(Page, QueryWrapper)} 之后仅用于本方法，还原后可继续安全复用。
     */
    private long selectCountWithOrderByStrippedTemporarily(QueryWrapper<T> ew) {
        MergeSegments exp = ew != null ? ew.getExpression() : null;
        List<ISqlSegment> savedOrder = null;
        if (exp != null) {
            savedOrder = new ArrayList<>(exp.getOrderBy());
            exp.getOrderBy().clear();
            invalidateMergeSegmentsSqlCache(exp);
        }
        clearQueryWrapperLastSql(ew);
        try {
            return baseMapper.selectCount(ew);
        } finally {
            if (exp != null && savedOrder != null) {
                exp.getOrderBy().clear();
                exp.getOrderBy().addAll(savedOrder);
                invalidateMergeSegmentsSqlCache(exp);
            }
        }
    }

    /**
     * {@link MergeSegments#getSqlSegment()} 在 {@code cacheSqlSegment==true} 时直接返回旧串；仅 clear orderBy 后必须失效缓存。
     */
    private static void invalidateMergeSegmentsSqlCache(MergeSegments merge) {
        if (merge == null) {
            return;
        }
        Field cache = ReflectionUtils.findField(MergeSegments.class, "cacheSqlSegment");
        if (cache != null) {
            ReflectionUtils.makeAccessible(cache);
            ReflectionUtils.setField(cache, merge, false);
        }
        // 子列表 clear 后父级仍可能 cacheSqlSegment==true 并返回含 ORDER BY 的旧串，导致 COUNT 带排序（Derby 等报错）
        Field seg = ReflectionUtils.findField(MergeSegments.class, "sqlSegment");
        if (seg != null) {
            ReflectionUtils.makeAccessible(seg);
            ReflectionUtils.setField(seg, merge, "");
        }
    }

    /**
     * 分页插件常把 LIMIT 写在 {@code lastSql}，COUNT 需去掉。
     */
    private static <E> void clearQueryWrapperLastSql(QueryWrapper<E> w) {
        if (w == null) {
            return;
        }
        try {
            Field f = ReflectionUtils.findField(AbstractWrapper.class, "lastSql");
            if (f == null) {
                return;
            }
            ReflectionUtils.makeAccessible(f);
            Object shared = ReflectionUtils.getField(f, w);
            if (shared != null) {
                Method toEmpty = shared.getClass().getMethod("toEmpty");
                toEmpty.invoke(shared);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(getPage(params));
    }

    /**
     * 默认逆序字段为<strong>逻辑列名</strong>（蛇形或与实体字段一致）：经 {@link WrapperColumns#orderByColumnExpression(String, boolean)}，
     * 与 {@link Query} 的 {@code sidx} 相同策略（{@link cn.org.autumn.xss.SQLFilter} + 方言引用）。已引用片段或复杂表达式规则见 {@link WrapperColumns}。
     * <p>
     * 排序写在 {@link Page#addOrder} 而非 {@link QueryWrapper#orderByDesc}：分页插件只在带 {@code IPage} 的查询上拼接
     * {@code ORDER BY}；{@link #selectCountWithoutOrderBy} 使用的 {@code ew} 无排序，Derby/DB2 等不会对 {@code COUNT(*)} 附加非法
     * {@code ORDER BY}。若仅清 Wrapper 的 orderBy 列表，{@link MergeSegments} 父级缓存仍可能返回旧片段，单靠反射易漏。
     * <p>
     * 须使用 {@link OrderItem#withExpression(String, boolean)}：{@link OrderItem#desc(String)} / {@link OrderItem#asc(String)} 会经
     * {@code setColumn} 调用 {@code replaceAllBlank} 去掉引号，Derby 等会得到 {@code ORDER BY id} 而非 {@code ORDER BY "id"}。
     */
    public PageUtils queryPage(Map<String, Object> params, List<String> descs) {
        Map<String, Object> normalized = normalizeQueryParams(params);
        Page<T> _page = new Query<T>(normalized).getPage();
        bindPageCondition(_page, getCondition(normalized));
        appendDescOrderItems(_page, descs);
        return queryPage(_page, new QueryWrapper<>());
    }

    public PageUtils queryPage(Map<String, Object> params, String... descs) {
        if (descs == null || descs.length == 0) {
            return queryPage(params, (List<String>) null);
        }
        return queryPage(params, Arrays.asList(descs));
    }

    public Page<T> getPage(Map<String, Object> params) {
        Map<String, Object> normalized = normalizeQueryParams(params);
        Page<T> _page = new Query<T>(normalized).getPage();
        bindPageCondition(_page, getCondition(normalized));
        return _page;
    }

    /**
     * 与 {@link #queryPage(Map, List)} 一致：将降序字段写入 {@link Page#addOrder}，使用 {@link OrderItem#withExpression(String, boolean)}
     * 以兼容 Derby/DB2/H2 等对方言引用列名的处理，并避免仅写在 {@link QueryWrapper} 上导致 COUNT 附带 ORDER BY。
     *
     * @param descs 降序排序列：优先传逻辑列名；已引用列名或复杂表达式可原样传入（规则同 {@link #queryPage(Map, List)}）
     */
    public Page<T> getPage(Map<String, Object> params, List<String> descs) {
        Map<String, Object> normalized = normalizeQueryParams(params);
        Page<T> _page = new Query<T>(normalized).getPage();
        bindPageCondition(_page, getCondition(normalized));
        appendDescOrderItems(_page, descs);
        return _page;
    }

    /**
     * 分页降序：{@link OrderItem#withExpression} 避免 {@link OrderItem#desc(String)} 去掉引号；逻辑列名经
     * {@link WrapperColumns#columnInWrapper(String)} 与 {@link Query} 中 {@code sidx} 对齐。
     */
    private void appendDescOrderItems(Page<T> page, List<String> descs) {
        if (page == null || descs == null) {
            return;
        }
        for (String desc : descs) {
            if (StringUtils.isNotBlank(desc)) {
                String col = WrapperColumns.orderByColumnExpression(desc, false);
                if (StringUtils.isNotBlank(col)) {
                    page.addOrder(OrderItem.withExpression(col, false));
                }
            }
        }
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
            wrapper.eq(WrapperColumns.columnInWrapper(entry.getKey()), value);
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
        if (descs == null || descs.length == 0) {
            return getPage(params, (List<String>) null);
        }
        return getPage(params, Arrays.asList(descs));
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