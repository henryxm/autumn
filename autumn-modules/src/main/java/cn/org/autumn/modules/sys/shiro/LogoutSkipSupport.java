package cn.org.autumn.modules.sys.shiro;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

/** 登出后短时跳过 {@code /sys/autologin} 与登录页 {@code checkenv}，避免开发环境静默重登。 */
public final class LogoutSkipSupport {

    public static final String COOKIE_NAME = "autumn_skip_autologin";

    /** 登出后跳过 autologin 的 Cookie 有效期（秒）。 */
    public static final int SKIP_MAX_AGE_SECONDS = 120;

    private LogoutSkipSupport() {
    }

    public static void mark(HttpServletRequest request, HttpServletResponse response) {
        if (request == null || response == null) {
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, "1");
        cookie.setMaxAge(SKIP_MAX_AGE_SECONDS);
        cookie.setPath(cookiePath(request));
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }

    public static boolean marked(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && "1".equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static void clear(HttpServletResponse response, HttpServletRequest request) {
        if (response == null || request == null) {
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath(cookiePath(request));
        response.addCookie(cookie);
    }

    private static String cookiePath(HttpServletRequest request) {
        String ctx = request.getContextPath();
        if (StringUtils.isBlank(ctx)) {
            return "/";
        }
        return ctx.endsWith("/") ? ctx : ctx + "/";
    }
}
