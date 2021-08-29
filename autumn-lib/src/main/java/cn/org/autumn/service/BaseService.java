package cn.org.autumn.service;

import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 基础服务类型
 *
 * @param <M> Mapper
 * @param <T> Entity
 */
public abstract class BaseService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseMenu {

    private Class<?> modelClass = null;
    private String prefix = null;

    public void setModelClass(Class<?> modelClass) {
        this.modelClass = modelClass;
    }

    public Class<?> getModelClass() {
        if (null != modelClass)
            return modelClass;
        Class<?> clazz = getClass();
        while (true) {
            Type type = clazz.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type[] types = parameterizedType.getActualTypeArguments();
                if (types.length == 2 && types[1] instanceof Class) {
                    Class<?> tmp = (Class<?>) types[1];
                    Table table = tmp.getAnnotation(Table.class);
                    if (null != table) {
                        modelClass = tmp;
                        break;
                    }
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz.equals(BaseService.class))
                break;
        }
        return modelClass;
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
        Page<T> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
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
        Map<String, Object> condition = getCondition(params);
        _page.setCondition(condition);
        return _page;
    }

    public Page<T> getPage(Map<String, Object> params, List<String> descs) {
        Page<T> _page = new Query<T>(params).getPage();
        Map<String, Object> condition = getCondition(params);
        _page.setCondition(condition);
        if (null != descs && descs.size() > 0)
            _page.setDescs(descs);
        return _page;
    }

    public Page<T> getPage(Map<String, Object> params, String... descs) {
        return getPage(params, new ArrayList<>(Arrays.asList(descs)));
    }

    public String getPrefix() {
        if (null == prefix) {
            Class<?> clazz = getModelClass();
            Table table = clazz.getAnnotation(Table.class);
            String tmp = table.prefix();
            if (StringUtils.isBlank(tmp) && StringUtils.isNotBlank(table.value()) && table.value().contains("_")) {
                tmp = table.value().split("_")[0];
            }
            prefix = tmp;
        }
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
        String pre = getPrefix();
        Class<?> clazz = getModelClass();
        String name = clazz.getSimpleName().toLowerCase();
        Table table = clazz.getAnnotation(Table.class);
        if (null == table)
            return null;
        if (name.endsWith("entity")) {
            name = name.substring(0, name.length() - 6);
        }
        String lang = table.value().replace(getPrefix() + "_", "");
        List<String[]> items = new ArrayList<>();
        String tableComment = table.comment();
        if (StringUtils.isNotBlank(tableComment) && tableComment.contains(":")) {
            tableComment = tableComment.split(":")[0];
        }
        items.add(new String[]{pre + "_" + name + "_table_comment", tableComment, toLang(lang)});
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
            items.add(new String[]{pre + "_" + name + "_column_" + underline, columnComment, toLang(underline)});
        }
        return items;
    }

    protected String[][] getMenuItemsInternal() {
        String pre = getPrefix();
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
                {tableComment, "modules/" + pre + "/" + name, pre + ":" + name + ":list," + pre + ":" + name + ":info," + pre + ":" + name + ":save," + pre + ":" + name + ":update," + pre + ":" + name + ":delete", "1", ico, order(), menu(), parentMenu(), pre + "_" + name + "_table_comment"},
                {"查看", null, pre + ":" + name + ":list," + pre + ":" + name + ":info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, pre + ":" + name + ":save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, pre + ":" + name + ":update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, pre + ":" + name + ":delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}