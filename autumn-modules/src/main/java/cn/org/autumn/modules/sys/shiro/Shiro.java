package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.config.Config;
import cn.org.autumn.config.VariablesHandler;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Component;

@Component
public class Shiro implements VariablesHandler {

    /**
     * 是否拥有该权限
     *
     * @param permission 权限标识
     * @return true：是     false：否
     */
    public boolean hasPermission(String permission) {
        Subject subject = SecurityUtils.getSubject();
        return subject != null && subject.isPermitted(permission);
    }

    /**
     * 判断用户是否登录
     *
     * @return true if user is login
     */
    public boolean isLogin() {
        return ShiroUtils.isLogin();
    }

    public SysUserEntity getUser() {
        return ShiroUtils.getUserEntity();
    }

    public boolean isProd() {
        return Config.isProd();
    }

    public boolean isDev() {
        return Config.isDev();
    }

    public boolean isTest() {
        return Config.isTest();
    }

    public String getEnv(String key) {
        return Config.getEnv(key);
    }

    public Object getBean(String name) {
        return Config.getBean(name);
    }

    public boolean isWindows() {
        return Config.windows();
    }

    public boolean isLinux() {
        return Config.linux();
    }

    public boolean isMacOS() {
        return Config.mac();
    }

    public String getOsName() {
        return System.getProperty("os.name");
    }

    public String getUserHome() {
        return System.getProperty("user.home");
    }

    public String getProperty(String key) {
        return System.getProperty(key);
    }
}