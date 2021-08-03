package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 业务系统实现PageHandler将根据Order进行从小到大的排序，并从小到大查找第一个不是空字符串的值为有效值
 */
@Component
@ConditionalOnMissingBean(PageHandler.class)
@Order
public interface PageHandler {
    default String getOauth2Login() {
        return "";
    }

    default String getLogin() {
        return "";
    }

    default String get404() {
        return "";
    }

    default int get404Status() {
        return 404;
    }

    default String getError() {
        return "";
    }

    default int getErrorStatus() {
        return 404;
    }

    default String getHeader() {
        return "";
    }

    default String getIndex() {
        return "";
    }

    default String getMain() {
        return "";
    }
}