package cn.org.autumn.model;

import org.apache.commons.beanutils.BeanUtils;

@SuppressWarnings("unchecked")
public interface EntityModel<T> extends Parameterized {
    default T copy() {
        try {
            Class<?> clazz = type(0);
            T t = (T) clazz.newInstance();
            BeanUtils.copyProperties(t, this);
            return t;
        } catch (Throwable ignored) {
        }
        return null;
    }
}