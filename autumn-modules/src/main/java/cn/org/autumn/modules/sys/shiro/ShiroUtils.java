package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.bot.shiro.RobotPrincipal;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
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

    public static Object getPrincipal() {
        try {
            return SecurityUtils.getSubject().getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    public static SysUserEntity getUserEntity() {
        Object principal = getPrincipal();
        if (principal instanceof SysUserEntity)
            return (SysUserEntity) principal;
        return null;
    }

    public static boolean isRobotLogin() {
        return getPrincipal() instanceof RobotPrincipal;
    }

    public static String getActorUuid() {
        Object principal = getPrincipal();
        if (principal instanceof RobotPrincipal)
            return ((RobotPrincipal) principal).getUuid();
        if (principal instanceof SysUserEntity)
            return ((SysUserEntity) principal).getUuid();
        return null;
    }

    public static String getOwnerUuid() {
        Object principal = getPrincipal();
        if (principal instanceof RobotPrincipal)
            return ((RobotPrincipal) principal).getOwner();
        if (principal instanceof SysUserEntity)
            return ((SysUserEntity) principal).getUuid();
        return null;
    }

    public static String getUserUuid() {
        SysUserEntity user = getUserEntity();
        if (user != null)
            return user.getUuid();
        return getActorUuid();
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
        } catch (Exception ignored) {
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
