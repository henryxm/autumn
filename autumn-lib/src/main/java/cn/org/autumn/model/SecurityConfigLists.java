package cn.org.autumn.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 安全加固 SysConfig 共用解析（列表拆分、SameSite 归一）。
 */
public final class SecurityConfigLists {

    private SecurityConfigLists() {
    }

    public static List<String> splitList(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("[,;\\n\\r]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    public static String normalizeSameSite(String raw, String fallback, List<String> fixes, String label) {
        if (StringUtils.isBlank(raw)) {
            return fallback;
        }
        String n = raw.trim().toUpperCase(Locale.ROOT);
        if ("NONE".equals(n) || "LAX".equals(n) || "STRICT".equals(n)) {
            return n;
        }
        if (fixes != null) {
            fixes.add(label + " invalid, reset to " + fallback + ": " + raw);
        }
        return fallback;
    }
}
