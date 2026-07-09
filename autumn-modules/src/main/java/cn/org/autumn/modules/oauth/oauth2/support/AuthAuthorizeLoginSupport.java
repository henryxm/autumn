package cn.org.autumn.modules.oauth.oauth2.support;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.springframework.ui.Model;

/**
 * OAuth/OPL 授权页登录方式 Tab 状态（account / qr / phone）。
 */
public final class AuthAuthorizeLoginSupport {

    public static final String SESSION_KEY = "oauth_authorize_login_tab";
    public static final String PARAM_LOGIN_TAB = "loginTab";
    public static final String TAB_ACCOUNT = "account";
    public static final String TAB_QR = "qr";
    public static final String TAB_PHONE = "phone";

    private AuthAuthorizeLoginSupport() {
    }

    public static void saveLoginTab(HttpServletRequest request, String tab) {
        String normalized = normalizeTab(tab);
        if (normalized == null) {
            return;
        }
        if (writeTabToSession(normalized)) {
            return;
        }
        if (request != null) {
            request.getSession(true).setAttribute(SESSION_KEY, normalized);
        }
    }

    public static void clearLoginTab(HttpServletRequest request) {
        removeTabFromSession();
        if (request == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    public static String resolveLoginTab(HttpServletRequest request) {
        String fromSession = readTabFromSession();
        if (fromSession != null) {
            return fromSession;
        }
        if (request != null) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                Object stored = httpSession.getAttribute(SESSION_KEY);
                if (stored != null) {
                    String tab = normalizeTab(String.valueOf(stored));
                    if (tab != null) {
                        return tab;
                    }
                }
            }
            String fromQuery = normalizeTab(request.getParameter(PARAM_LOGIN_TAB));
            if (fromQuery != null) {
                return fromQuery;
            }
        }
        return TAB_ACCOUNT;
    }

    public static void enrichLoggedInUser(HttpServletRequest request, Model model, SysUserEntity user) {
        if (model == null || user == null) {
            return;
        }
        String tab = resolveLoginTab(request);
        model.addAttribute("authorizeLoginTab", tab);
        String displayName = StringUtils.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        if (StringUtils.isBlank(displayName)) {
            displayName = user.getUuid();
        }
        model.addAttribute("loginUserName", displayName);
        model.addAttribute("loginUserNickname", StringUtils.defaultString(user.getNickname()));
        model.addAttribute("loginUserIcon", StringUtils.defaultString(user.getIcon()));
        model.addAttribute("loginUserMobile", maskMobile(user.getMobile()));
    }

    public static boolean isAuthorizeCallback(String callbackUrl) {
        if (StringUtils.isBlank(callbackUrl)) {
            return false;
        }
        String path = callbackUrl.trim();
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                URI uri = URI.create(path);
                path = uri.getPath();
                if (StringUtils.isNotBlank(uri.getQuery()) && uri.getQuery().contains("client_id=")) {
                    return true;
                }
                if (StringUtils.isNotBlank(uri.getQuery()) && uri.getQuery().contains("app_id=")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        return path.contains("/oauth2/authorize") || path.contains("/open/oauth2/authorize");
    }

    public static String resolveLoginTabForLogin(HttpServletRequest request, String username) {
        String fromForm = normalizeTab(request != null ? request.getParameter(PARAM_LOGIN_TAB) : null);
        if (fromForm != null) {
            return fromForm;
        }
        if (request != null && StringUtils.isNotBlank(username) && username.matches("^1\\d{10}$")) {
            return TAB_PHONE;
        }
        return TAB_ACCOUNT;
    }

    public static String appendLoginTab(String url, String tab) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(normalizeTab(tab))) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + PARAM_LOGIN_TAB + "=" + normalizeTab(tab);
    }

    static String normalizeTab(String tab) {
        if (StringUtils.isBlank(tab)) {
            return null;
        }
        String t = tab.trim().toLowerCase();
        if (TAB_ACCOUNT.equals(t) || TAB_QR.equals(t) || TAB_PHONE.equals(t)) {
            return t;
        }
        return null;
    }

    static String maskMobile(String mobile) {
        if (StringUtils.isBlank(mobile) || mobile.length() < 7) {
            return StringUtils.defaultString(mobile);
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private static boolean writeTabToSession(String tab) {
        try {
            if (ShiroUtils.isLogin()) {
                ShiroUtils.setSessionAttribute(SESSION_KEY, tab);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void removeTabFromSession() {
        try {
            Session session = ShiroUtils.getSubject().getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_KEY);
            }
        } catch (Exception ignored) {
        }
    }

    private static String readTabFromSession() {
        try {
            Session session = ShiroUtils.getSubject().getSession(false);
            if (session != null) {
                Object stored = session.getAttribute(SESSION_KEY);
                if (stored != null) {
                    return normalizeTab(String.valueOf(stored));
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
