package cn.org.autumn.modules.open.support;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/** 开放模块管理端分页查询公共工具。 */
public final class AdminPageQueries {

    private AdminPageQueries() {
    }

    public static String stringParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return null;
        }
        return params.get(key).toString();
    }

    public static void applyKeyword(EntityWrapper<?> wrapper, Map<String, Object> params, String... columns) {
        String keyword = stringParam(params, "keyword");
        if (StringUtils.isBlank(keyword) || columns == null || columns.length == 0) {
            return;
        }
        String value = keyword.trim();
        wrapper.andNew();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                wrapper.or();
            }
            wrapper.like(columns[i], value);
        }
    }
}
