package cn.org.autumn.modules.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

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

    public static void applyKeyword(QueryWrapper<?> wrapper, Map<String, Object> params, String... columns) {
        String keyword = stringParam(params, "keyword");
        if (StringUtils.isBlank(keyword) || columns == null || columns.length == 0) {
            return;
        }
        String value = keyword.trim();
        wrapper.and(w -> {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    w.or();
                }
                w.like(columns[i], value);
            }
        });
    }
}
