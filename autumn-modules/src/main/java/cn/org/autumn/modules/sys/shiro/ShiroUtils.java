package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.exception.AException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

public class ShiroUtils {
    /**
     * 加密算法
     */
    public final static String hashAlgorithmName = "SHA-256";
    /**
     * 循环次数
     */
    public final static int hashIterations = 16;

    public static String sha256(String password, String salt) {
        return new SimpleHash(hashAlgorithmName, password, salt, hashIterations).toString();
    }

    public static Session getSession() {
        return SecurityUtils.getSubject().getSession();
    }

    public static Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    public static SysUserEntity getUserEntity() {
        return (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
    }

    public static String getUserUuid() {
        return getUserEntity().getUuid();
    }

    public static void setSessionAttribute(Object key, Object value) {
        getSession().setAttribute(key, value);
    }

    public static Object getSessionAttribute(Object key) {
        return getSession().getAttribute(key);
    }

    public static boolean isLogin() {
        try {
            return SecurityUtils.getSubject().getPrincipal() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAuthenticated() {
        return SecurityUtils.getSubject().isAuthenticated();
    }

    public static boolean needLogin() {
        return !isLogin() && !isAuthenticated();
    }

    public static void logout() {
        try {
            SecurityUtils.getSubject().logout();
        } catch (Exception e) {
        }
    }

    public static String getKaptcha(String key) {
        Object kaptcha = getSessionAttribute(key);
        if (kaptcha == null) {
            throw new AException("验证码已失效");
        }
        getSession().removeAttribute(key);
        return kaptcha.toString();
    }
}
