package cn.org.autumn.node.role;

import cn.org.autumn.config.Config;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 将请求路径映射为所需能力；空 roles / ALL 时调用方短路，本类只负责分类。
 */
@Component
public class ServerRolePathClassifier {

    public static final String CFG_API_PREFIXES = "autumn.node.role.api-path-prefixes";
    public static final String CFG_WEB_PREFIXES = "autumn.node.role.web-path-prefixes";
    public static final String CFG_DOWNLOAD_PREFIXES = "autumn.node.role.download-path-prefixes";

    private static final List<String> DEFAULT_API = List.of(
            "/api/", "/openapi/", "/v1/", "/v2/", "/v3/", "/sys/login", "/sys/oauth/", "/oauth/");
    private static final List<String> DEFAULT_DOWNLOAD = List.of(
            "/download/", "/file/", "/files/", "/upload/", "/statics/", "/static/", "/webjars/");
    private static final List<String> DEFAULT_WEB = List.of(
            "/modules/", "/sys/", "/admin/", "/pages/");

    /**
     * @return 所需能力 code；{@code null} 表示本拦截器不限制（放行）
     */
    public String requiredCapability(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (isAlwaysOpen(p)) {
            return null;
        }
        for (String prefix : downloadPrefixes()) {
            if (pathMatches(p, prefix)) {
                return ServerRole.CAP_FILE_DOWNLOAD;
            }
        }
        for (String prefix : apiPrefixes()) {
            if (pathMatches(p, prefix)) {
                return ServerRole.CAP_API_HTTP;
            }
        }
        for (String prefix : webPrefixes()) {
            if (pathMatches(p, prefix)) {
                return ServerRole.CAP_WEB_UI;
            }
        }
        // 未分类路径：有明确角色限制时按 WEB_UI 保守处理（避免 API 节点误开站点）
        if (!ServerRoleGate.isUnrestricted()) {
            if (looksLikePage(p)) {
                return ServerRole.CAP_WEB_UI;
            }
        }
        return null;
    }

    static boolean isAlwaysOpen(String p) {
        return p.equals("/")
                || p.equals("/error")
                || p.startsWith("/error/")
                || p.equals("/favicon.ico")
                || p.startsWith("/client/")
                || p.startsWith("/install")
                || p.startsWith("/actuator/");
    }

    static boolean looksLikePage(String p) {
        return !p.contains(".") || p.endsWith(".html") || p.endsWith(".htm");
    }

    static boolean pathMatches(String path, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return false;
        }
        String pre = prefix.trim();
        if (!pre.startsWith("/")) {
            pre = "/" + pre;
        }
        if (pre.endsWith("/**")) {
            pre = pre.substring(0, pre.length() - 3);
            return path.equals(pre) || path.startsWith(pre.endsWith("/") ? pre : pre + "/") || path.startsWith(pre);
        }
        if (pre.endsWith("/")) {
            return path.startsWith(pre) || path.equals(pre.substring(0, pre.length() - 1));
        }
        return path.equals(pre) || path.startsWith(pre + "/");
    }

    private List<String> apiPrefixes() {
        return configuredOrDefault(CFG_API_PREFIXES, DEFAULT_API);
    }

    private List<String> webPrefixes() {
        return configuredOrDefault(CFG_WEB_PREFIXES, DEFAULT_WEB);
    }

    private List<String> downloadPrefixes() {
        return configuredOrDefault(CFG_DOWNLOAD_PREFIXES, DEFAULT_DOWNLOAD);
    }

    private static List<String> configuredOrDefault(String key, List<String> defaults) {
        String raw = Config.getEnv(key);
        if (StringUtils.isBlank(raw)) {
            return defaults;
        }
        List<String> list = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            if (StringUtils.isNotBlank(part)) {
                list.add(part.trim());
            }
        }
        return list.isEmpty() ? defaults : list;
    }
}
