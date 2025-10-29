package cn.org.autumn.search;

import cn.org.autumn.annotation.SearchType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

public interface ISearch extends Serializable {
    default List<String> types() {
        return null;
    }

    default String text() {
        return null;
    }

    default Object param() {
        return null;
    }

    default boolean isPureDigits() {
        return isPureDigits(text());
    }

    static boolean isPureDigits(String text) {
        if (text == null) {
            return false;
        }
        int length = text.length();
        // 快速长度检查
        if (length == 0 || length > 50) {
            return false;
        }
        // 使用位运算优化的数字检查
        for (int i = 0; i < length; i++) {
            int c = text.charAt(i);
            // 使用位运算快速判断是否为数字字符 '0'(48) 到 '9'(57)
            if ((c - 48) >>> 4 != 0 || (c - 48) > 9) {
                return false;
            }
        }
        return true;
    }

    static String getType(Class<?> clazz) {
        if (null == clazz)
            return null;
        SearchType searchType = clazz.getDeclaredAnnotation(SearchType.class);
        if (null != searchType && StringUtils.isNotBlank(searchType.value()))
            return searchType.value();
        return clazz.getSimpleName();
    }
}
