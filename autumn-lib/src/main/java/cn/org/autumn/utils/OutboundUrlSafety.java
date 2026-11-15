package cn.org.autumn.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 出站 URL 安全校验：防 SSRF（协议、私网/元数据地址、可选主机白名单）。
 * <p>
 * 供 HTTP 透明代理、Webhook 投递等复用。白名单为空时拒绝全部目标（安全默认）。
 */
public final class OutboundUrlSafety {

    public static final String REASON_DISABLED = "proxy_disabled";
    public static final String REASON_BLANK = "blank_url";
    public static final String REASON_INVALID = "invalid_url";
    public static final String REASON_SCHEME = "scheme_not_allowed";
    public static final String REASON_HOST_BLANK = "host_blank";
    public static final String REASON_ALLOWLIST = "host_not_allowlisted";
    public static final String REASON_PRIVATE = "private_or_metadata_address";
    public static final String REASON_RESOLVE = "host_resolve_failed";

    private OutboundUrlSafety() {
    }

    /**
     * @param url           完整目标 URL
     * @param enabled       false 时一律拒绝
     * @param allowedHosts  允许的主机（精确或 {@code .suffix.com} / {@code *.suffix.com} 后缀）；空集合=拒绝全部
     * @param resolveDns    是否解析 DNS 并校验解析结果是否私网
     * @return null 表示通过；否则为拒绝原因码
     */
    public static String rejectReason(String url, boolean enabled, Collection<String> allowedHosts, boolean resolveDns) {
        if (!enabled) {
            return REASON_DISABLED;
        }
        if (StringUtils.isBlank(url)) {
            return REASON_BLANK;
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            return REASON_INVALID;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return REASON_SCHEME;
        }
        String schemeLower = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(schemeLower) && !"https".equals(schemeLower)) {
            return REASON_SCHEME;
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            // URI.create 对部分 IPv6 可能无 host，尝试 URL 解析
            try {
                host = uri.toURL().getHost();
            } catch (Exception ignored) {
                return REASON_HOST_BLANK;
            }
        }
        if (StringUtils.isBlank(host)) {
            return REASON_HOST_BLANK;
        }
        String hostLower = host.toLowerCase(Locale.ROOT);
        if (isLiteralPrivateOrMetadataHost(hostLower)) {
            return REASON_PRIVATE;
        }
        if (!hostAllowlisted(hostLower, allowedHosts)) {
            return REASON_ALLOWLIST;
        }
        if (resolveDns) {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                if (addrs == null || addrs.length == 0) {
                    return REASON_RESOLVE;
                }
                for (InetAddress addr : addrs) {
                    if (isBlockedAddress(addr)) {
                        return REASON_PRIVATE;
                    }
                }
            } catch (UnknownHostException e) {
                return REASON_RESOLVE;
            }
        }
        return null;
    }

    public static boolean isAllowed(String url, boolean enabled, Collection<String> allowedHosts, boolean resolveDns) {
        return rejectReason(url, enabled, allowedHosts, resolveDns) == null;
    }

    /**
     * Webhook 等：默认启用、可配置白名单；白名单为空时仅拒绝私网（允许任意公网主机）。
     * 代理场景请用 {@link #rejectReason} 且白名单为空则拒绝。
     */
    public static String rejectReasonPublicHostsOk(String url, Collection<String> allowedHosts, boolean resolveDns) {
        if (StringUtils.isBlank(url)) {
            return REASON_BLANK;
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            return REASON_INVALID;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return REASON_SCHEME;
        }
        String schemeLower = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(schemeLower) && !"https".equals(schemeLower)) {
            return REASON_SCHEME;
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            try {
                host = uri.toURL().getHost();
            } catch (Exception ignored) {
                return REASON_HOST_BLANK;
            }
        }
        if (StringUtils.isBlank(host)) {
            return REASON_HOST_BLANK;
        }
        String hostLower = host.toLowerCase(Locale.ROOT);
        if (isLiteralPrivateOrMetadataHost(hostLower)) {
            return REASON_PRIVATE;
        }
        if (allowedHosts != null && !allowedHosts.isEmpty() && !hostAllowlisted(hostLower, allowedHosts)) {
            return REASON_ALLOWLIST;
        }
        if (resolveDns) {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                if (addrs == null || addrs.length == 0) {
                    return REASON_RESOLVE;
                }
                for (InetAddress addr : addrs) {
                    if (isBlockedAddress(addr)) {
                        return REASON_PRIVATE;
                    }
                }
            } catch (UnknownHostException e) {
                return REASON_RESOLVE;
            }
        }
        return null;
    }

    static boolean hostAllowlisted(String hostLower, Collection<String> allowedHosts) {
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            return false;
        }
        for (String raw : allowedHosts) {
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            String pattern = raw.trim().toLowerCase(Locale.ROOT);
            if (pattern.startsWith("*.")) {
                String suffix = pattern.substring(1); // .example.com
                if (hostLower.endsWith(suffix) || hostLower.equals(pattern.substring(2))) {
                    return true;
                }
            } else if (pattern.startsWith(".")) {
                if (hostLower.endsWith(pattern) || hostLower.equals(pattern.substring(1))) {
                    return true;
                }
            } else if (hostLower.equals(pattern) || hostLower.endsWith("." + pattern)) {
                return true;
            }
        }
        return false;
    }

    static boolean isLiteralPrivateOrMetadataHost(String hostLower) {
        if ("localhost".equals(hostLower) || "metadata.google.internal".equals(hostLower)) {
            return true;
        }
        if ("169.254.169.254".equals(hostLower) || "metadata".equals(hostLower)) {
            return true;
        }
        // IPv4 literal
        if (hostLower.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            try {
                return isBlockedAddress(InetAddress.getByName(hostLower));
            } catch (UnknownHostException e) {
                return true;
            }
        }
        // IPv6 literal without brackets already stripped by URI
        if (hostLower.contains(":")) {
            try {
                return isBlockedAddress(InetAddress.getByName(hostLower));
            } catch (UnknownHostException e) {
                return true;
            }
        }
        return false;
    }

    static boolean isBlockedAddress(InetAddress addr) {
        if (addr == null) {
            return true;
        }
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int a0 = b[0] & 0xff;
            int a1 = b[1] & 0xff;
            // 169.254.0.0/16 link-local already covered; cloud metadata
            if (a0 == 169 && a1 == 254) {
                return true;
            }
            // 100.64.0.0/10 CGNAT
            if (a0 == 100 && a1 >= 64 && a1 <= 127) {
                return true;
            }
            // 0.0.0.0/8
            if (a0 == 0) {
                return true;
            }
        }
        if (b.length == 16) {
            // fc00::/7 unique local
            if ((b[0] & 0xfe) == 0xfc) {
                return true;
            }
            // ::ffff:0:0/96 mapped — check embedded IPv4
            boolean v4mapped = b[0] == 0 && b[1] == 0 && b[2] == 0 && b[3] == 0
                    && b[4] == 0 && b[5] == 0 && b[6] == 0 && b[7] == 0
                    && b[8] == 0 && b[9] == 0 && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
            if (v4mapped) {
                try {
                    byte[] v4 = new byte[]{b[12], b[13], b[14], b[15]};
                    return isBlockedAddress(InetAddress.getByAddress(v4));
                } catch (UnknownHostException e) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 便于测试的固定列表包装 */
    public static List<String> hosts(String... hosts) {
        return hosts == null ? List.of() : List.of(hosts);
    }
}
