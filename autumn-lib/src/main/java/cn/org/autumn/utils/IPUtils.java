package cn.org.autumn.utils;

import lombok.extern.slf4j.Slf4j;
import cn.org.autumn.config.Config;
import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * IP地址
 *
 * @author mac
 */
@Slf4j
public class IPUtils {
    private final static String IPV6_REGEX = "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$";
    /**
     * Docker / 环境变量标准键，如 {@code HOST_LAN_IP=192.168.0.155}
     */
    public static final String HOST_LAN_IP_ENV = "HOST_LAN_IP";
    /**
     * JVM / Spring / compose 点分键，如 {@code host.lan.ip=192.168.0.155}
     */
    public static final String HOST_LAN_IP_PROP = "host.lan.ip";

    /**
     * 可信反向代理列表（逗号/空白分隔的 IP 或 IPv4 CIDR）。
     * 未配置时 {@link #getIp(HttpServletRequest)} <strong>忽略</strong> X-Forwarded-For 等头，仅用 {@code remoteAddr}，
     * 防止客户端伪造管理 IP / 防火墙身份。
     */
    public static final String TRUSTED_PROXIES_PROP = "autumn.client-ip.trusted-proxies";
    /** 同 {@link #TRUSTED_PROXIES_PROP} 的环境变量风格键。 */
    public static final String TRUSTED_PROXIES_ENV = "AUTUMN_CLIENT_IP_TRUSTED_PROXIES";

    /** 管理白名单 IPv4 CIDR 允许的最宽前缀（越大越窄；/24 = 最多 256 地址）。 */
    public static final int ADMIN_IPV4_MIN_PREFIX = 24;
    /** 管理白名单 IPv6 CIDR 允许的最宽前缀。 */
    public static final int ADMIN_IPV6_MIN_PREFIX = 64;

    //判断是否字符串是ipv6
    public static boolean isIPV6(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        return Pattern.compile(IPV6_REGEX, Pattern.CASE_INSENSITIVE).matcher(ip).matches();
    }

    public static boolean isIPV6Address(String address) {
        if (StringUtils.isBlank(address)) {
            return false;
        }
        Pattern pattern1 = Pattern.compile("\\[\\S+\\]:\\d+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern pattern2 = Pattern.compile("\\S+:\\S+:\\d+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return pattern1.matcher(address).matches() || pattern2.matcher(address).matches();
    }

    /**
     * 解析客户端 IP。
     * <p>
     * 配置 {@link #TRUSTED_PROXIES_PROP} 后：仅当 TCP 对端 ∈ 可信列表时，才从 XFF <strong>右侧</strong>剥离可信跳。
     * 未配置时：公网 {@code remoteAddr} <strong>忽略</strong> XFF（防伪造）；若 {@code remoteAddr} 为回环/私网
     * （典型本机 Nginx / Docker 网桥），则信任 XFF <strong>最右侧一跳</strong>为客户端（须由边缘 Nginx
     * <strong>覆盖</strong> {@code X-Forwarded-For $remote_addr}，勿 append 客户端原值）。
     */
    public static String getIp(HttpServletRequest request) {
        if (null == request) {
            return "";
        }
        String remote = trimHop(request.getRemoteAddr());
        List<String> trusted = trustedProxyEntries();
        if (trusted.isEmpty()) {
            if (!isInternalKeepIp(remote)) {
                return remote;
            }
            return clientIpFromPrivateRemote(request, remote);
        }
        if (!isTrustedProxyHop(remote, trusted)) {
            return remote;
        }
        return stripTrustedFromXff(request, remote, trusted);
    }

    /**
     * 未配 trusted-proxies、且 TCP 对端为私网/回环时：取 XFF 最右有效 hop（一跳）；无则 X-Real-IP；再无则 remote。
     */
    private static String clientIpFromPrivateRemote(HttpServletRequest request, String remote) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(xff)) {
            String[] hops = xff.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = trimHop(hops[i]);
                if (hop.isEmpty() || "unknown".equalsIgnoreCase(hop)) {
                    continue;
                }
                return hop;
            }
        }
        String real = trimHop(request.getHeader("X-Real-IP"));
        if (StringUtils.isNotBlank(real) && !"unknown".equalsIgnoreCase(real)) {
            return real;
        }
        return remote;
    }

    private static String stripTrustedFromXff(HttpServletRequest request, String remote, List<String> trusted) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(xff)) {
            String real = trimHop(request.getHeader("X-Real-IP"));
            if (StringUtils.isNotBlank(real) && !"unknown".equalsIgnoreCase(real)
                    && !isTrustedProxyHop(real, trusted)) {
                return real;
            }
            return remote;
        }
        String[] hops = xff.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = trimHop(hops[i]);
            if (hop.isEmpty() || "unknown".equalsIgnoreCase(hop)) {
                continue;
            }
            if (!isTrustedProxyHop(hop, trusted)) {
                return hop;
            }
        }
        return remote;
    }

    /** 读取可信代理配置（可测：空 = 不信任转发头）。 */
    public static List<String> trustedProxyEntries() {
        String raw = Config.getEnv(TRUSTED_PROXIES_ENV);
        if (StringUtils.isBlank(raw)) {
            raw = Config.getEnv(TRUSTED_PROXIES_PROP);
        }
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            String p = part == null ? "" : part.trim();
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    public static boolean isTrustedProxyHop(String hop, List<String> trusted) {
        if (StringUtils.isBlank(hop) || trusted == null || trusted.isEmpty()) {
            return false;
        }
        for (String entry : trusted) {
            if (entry.contains("/")) {
                if (isIp(hop) && isInRange(hop, entry)) {
                    return true;
                }
            } else if (entry.equalsIgnoreCase(hop)) {
                return true;
            }
        }
        return false;
    }

    private static String trimHop(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 校验管理白名单条目：单 IP 或足够窄的 CIDR（IPv4 ≥ /24，IPv6 ≥ /64）。
     *
     * @return 规范化条目；非法返回 null
     */
    public static String normalizeAdminWhiteEntry(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String ip = raw.trim();
        if (!ip.contains("/")) {
            if (isIp(ip) || isIPV6(ip)) {
                return ip;
            }
            return null;
        }
        int slash = ip.lastIndexOf('/');
        if (slash <= 0 || slash >= ip.length() - 1) {
            return null;
        }
        String host = ip.substring(0, slash).trim();
        String prefixStr = ip.substring(slash + 1).trim();
        int prefix;
        try {
            prefix = Integer.parseInt(prefixStr);
        } catch (NumberFormatException e) {
            return null;
        }
        if (isIp(host)) {
            if (prefix < ADMIN_IPV4_MIN_PREFIX || prefix > 32) {
                return null;
            }
            return host + "/" + prefix;
        }
        if (isIPV6(host)) {
            if (prefix < ADMIN_IPV6_MIN_PREFIX || prefix > 128) {
                return null;
            }
            return host + "/" + prefix;
        }
        return null;
    }

    public static boolean isIp(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        return ip.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}");
    }

    /**
     * ip 地址所属网段判断（仅 IPv4）。前缀须为 1～32；{@code /0} 不匹配任何地址（避免 int 移位回绕）。
     */
    public static boolean isInRange(String ip, String cidr) {
        if (!isIp(ip) || StringUtils.isBlank(cidr) || !cidr.contains("/")) {
            return false;
        }
        int slash = cidr.lastIndexOf('/');
        String cidrIp = cidr.substring(0, slash).trim();
        String prefixStr = cidr.substring(slash + 1).trim();
        if (!isIp(cidrIp)) {
            return false;
        }
        int type;
        try {
            type = Integer.parseInt(prefixStr);
        } catch (NumberFormatException e) {
            return false;
        }
        if (type < 1 || type > 32) {
            return false;
        }
        String[] ips = ip.split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24)
                | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) | Integer.parseInt(ips[3]);
        int mask = type == 32 ? 0xFFFFFFFF : (0xFFFFFFFF << (32 - type));
        String[] cidrIps = cidrIp.split("\\.");
        int cidrIpAddr = (Integer.parseInt(cidrIps[0]) << 24)
                | (Integer.parseInt(cidrIps[1]) << 16)
                | (Integer.parseInt(cidrIps[2]) << 8)
                | Integer.parseInt(cidrIps[3]);
        return (ipAddr & mask) == (cidrIpAddr & mask);
    }

    /**
     * 获取宿主机局域网 IP（Docker 等场景由外部手动指定，避免探测到容器网桥 IP）。
     * <p>
     * 读取顺序（经 {@link Config#getEnv}）：环境变量 → 系统属性 → Spring Environment（含 yml）。
     * 优先 {@link #HOST_LAN_IP_ENV}，其次 {@link #HOST_LAN_IP_PROP}。
     * <p>
     * Docker Compose 示例：
     * <pre>
     *   environment:
     *     - HOST_LAN_IP=192.168.0.155
     *     # 或
     *     - host.lan.ip=192.168.0.155
     * </pre>
     * application.yml / properties：{@code host.lan.ip: 192.168.0.155}
     *
     * @return 有效 IPv4；未配置或非法时返回空字符串
     */
    public static String getHostLanIp() {
        String ip = Config.getEnv(HOST_LAN_IP_ENV);
        if (StringUtils.isBlank(ip)) {
            ip = Config.getEnv(HOST_LAN_IP_PROP);
        }
        if (StringUtils.isBlank(ip)) {
            return "";
        }
        ip = ip.trim();
        if (!isIp(ip)) {
            log.warn("Invalid HOST_LAN_IP config: {}", ip);
            return "";
        }
        return ip;
    }

    /**
     * 本机对外 IP：优先 {@link #getHostLanIp()}，否则网卡探测，再退回 127.0.0.1 / localhost。
     */
    public static String getIp() {
        try {
            String configured = getHostLanIp();
            if (StringUtils.isNotBlank(configured)) {
                return configured;
            }
            String ip = null;
            InetAddress address = getLocalHostLANAddress();
            if (null == address) {
                address = InetAddress.getLocalHost();
            }
            if (null != address) {
                ip = address.getHostAddress();
            }
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";
            }
            return ip;
        } catch (Exception e) {
            return "localhost";
        }
    }

    public static InetAddress getLocalHostLANAddress() {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        // 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isInternalKeepIp(String ip) {
        if (ip == null) return false;
        ip = ip.trim();
        // IPv4 localhost
        if (ip.equals("127.0.0.1") || ip.equals("0.0.0.0")) return true;
        // IPv6 localhost
        if (ip.equals("::1")) return true;
        // IPv4 private
        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) return true;
                } catch (Exception ignored) {
                }
            }
        }
        // IPv4 link-local
        if (ip.startsWith("169.254.")) return true;
        // IPv4 multicast
        try {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                int first = Integer.parseInt(parts[0]);
                if (first >= 224 && first <= 239) return true; // 224.0.0.0 - 239.255.255.255
                if (first >= 240 && first <= 255) return true; // 240.0.0.0 - 255.255.255.254 (reserved)
            }
        } catch (Exception ignored) {
        }
        // IPv6 unique local (fc00::/7)
        if (ip.toLowerCase().startsWith("fc") || ip.toLowerCase().startsWith("fd")) return true;
        // IPv6 link-local (fe80::/10)
        if (ip.toLowerCase().startsWith("fe8") || ip.toLowerCase().startsWith("fe9") || ip.toLowerCase().startsWith("fea") || ip.toLowerCase().startsWith("feb"))
            return true;
        // IPv6 multicast (ff00::/8)
        if (ip.toLowerCase().startsWith("ff")) return true;
        // IPv6 unspecified (::)
        if (ip.equals("::")) return true;
        return false;
    }
}
