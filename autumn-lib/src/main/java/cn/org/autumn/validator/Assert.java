package cn.org.autumn.validator;

import cn.org.autumn.exception.AException;
import org.apache.commons.lang.StringUtils;

/**
 * 数据校验
 */
public abstract class Assert {

    public static void isBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new AException(message);
        }
    }

    public static void isNull(Object object, String message) {
        if (object == null) {
            throw new AException(message);
        }
    }
}
