package cn.org.autumn.modules.install;

import org.apache.commons.lang.StringUtils;

/**
 * 安装向导展示用 JDBC 脱敏（不含密码回显）。
 */
public final class InstallJdbcUrlMask {

    private static final int MAX_LEN = 200;

    private InstallJdbcUrlMask() {
    }

    public static String mask(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String u = url.trim();
        u = u.replaceAll("(?i)(//)([^/@:]+)(:)([^/@]+)(@)", "$1$2$3***$5");
        u = u.replaceAll("(?i)([?&]password=)([^&]*)", "$1***");
        if (u.length() > MAX_LEN) {
            return u.substring(0, MAX_LEN - 3) + "...";
        }
        return u;
    }
}
