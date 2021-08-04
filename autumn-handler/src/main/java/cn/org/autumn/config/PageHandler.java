package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 业务系统实现PageHandler将根据Order进行从小到大的排序，并从小到大查找第一个不是空字符串的值为有效值
 */
@Component
@ConditionalOnMissingBean(PageHandler.class)
@Order(Integer.MAX_VALUE / 100)
public interface PageHandler {
    default String oauth2Login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String header(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }
}