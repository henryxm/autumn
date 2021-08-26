package cn.org.autumn.service;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

    public Map<String, Object> getCondition(Map<String, Object> params) {
        Map<String, Object> condition = new HashMap<>();
        Class<?> clazz = getClass();
        Class<?> entity = null;
        while (true) {
            Type type = clazz.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type[] types = parameterizedType.getActualTypeArguments();
                if (types.length == 2 && types[1] instanceof Class) {
                    entity = (Class<?>) types[1];
                    Table table = entity.getAnnotation(Table.class);
                    if (null != table)
                        break;
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz.equals(BaseService.class))
                break;
        }
        if (null == entity)
            return condition;

        Field[] fields = clazz.getDeclaredFields();
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
            if (null != value) {
                condition.put(columnName, value);
            }
        }
        return condition;
    }

    public PageUtils queryPage(Page<T> _page) {
        EntityWrapper<T> entityEntityWrapper = new EntityWrapper<>();
        Page<T> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(getPage(params));
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
        _page.setDescs(descs);
        return _page;
    }
}