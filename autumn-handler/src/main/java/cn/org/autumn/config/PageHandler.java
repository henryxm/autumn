package cn.org.autumn.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 业务系统实现 PageHandler 将根据 Order 从小到大排序，取第一个非空模板名作为有效值。
 */
@Component
@ConditionalOnMissingBean(PageHandler.class)
@Order(Integer.MAX_VALUE / 100)
public interface PageHandler {

    default String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String register(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String forgotPassword(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String direct(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String _505(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String _500(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
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

    default String loading(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    default String scan(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** OAuth AS 授权页（登录 + 确认，{@code /oauth2/authorize}）。 */
    default String oauthAuthorize(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** OPL AS 开放平台授权页（登录 + 确认，{@code /open/oauth2/authorize?app_id=}）。 */
    default String oplAuthorize(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** OAuth 客户端登录入口页（{@code /oauth2/login?client_id=}）。 */
    default String oauthLoginEntry(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** OAuth 客户端登录成功页（{@code /oauth2/success}）。 */
    default String oauthLoginSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** Open 接入登录入口页（{@code /open/oauth2/login?appId=}）。 */
    default String openLoginEntry(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** Open 接入登录成功页（{@code /open/oauth2/success}）。 */
    default String openLoginSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** OAuth / Open 授权回调失败页。 */
    default String authCallbackError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }

    /** OAuth 授权后未登录时的绑定方式选择页。 */
    default String oauthBindChoice(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "";
    }
}
