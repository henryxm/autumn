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
     * 可选：可信反向代理列表（逗号/空白分隔的 IP 或 IPv4 CIDR）。
     * <p>
     * <strong>未配置（默认）</strong>：{@link #getIp(HttpServletRequest)} 沿用历史语义——优先
     * {@code X-Forwarded-For} <strong>最左</strong>有效 hop，再回退其它代理头 / {@code remoteAddr}
     * （阿里云 SLB+Nginx 等场景与加固前行为一致，避免把 {@code 100.125.x} 当作用户 IP）。
     * <p>
     * <strong>已配置</strong>：进入防伪造严格模式——仅当 TCP 对端 ∈ 列表时才从 XFF 右侧剥离可信跳
     * （适用于直连公网、需防客户端伪造管理 IP 的部署；须含本机 Nginx 与云 SLB 网段，如
     * {@code 100.64.0.0/10}）。
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
     * 解析客户端 IP（登录日志、防火墙、管理 IP 认领等共用）。
     * <p>
     * 默认与加固前一致：见 {@link #classicClientIp(HttpServletRequest)}。
     * 仅当显式配置 {@link #TRUSTED_PROXIES_PROP} 时启用右侧剥离严格模式（防直连伪造）。
     */
    public static String getIp(HttpServletRequest request) {
        if (null == request) {
            return "";
        }
        List<String> trusted = trustedProxyEntries();
        if (trusted.isEmpty()) {
            return classicClientIp(request);
        }
        String remote = trimHop(request.getRemoteAddr());
        if (!isTrustedProxyHop(remote, trusted)) {
            return remote;
        }
        return stripTrustedFromXff(request, remote, trusted);
    }

    /**
     * 历史默认语义（510ab885 之前）：优先 XFF 最左有效 hop，再回退常见代理头，最后 {@code remoteAddr}。
     * 云 SLB 场景下最左一般为真实用户，右侧常为 {@code 100.125.x} 等基建地址。
     */
    public static String classicClientIp(HttpServletRequest request) {
        if (null == request) {
            return "";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        }
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String s : ips) {
                String hop = trimHop(s);
                if (!hop.isEmpty() && !"unknown".equalsIgnoreCase(hop)) {
                    return hop;
                }
            }
        } else if (ip != null) {
            ip = ip.trim();
        }
        return ip == null ? "" : ip;
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

    /**
     * 基础设施 hop：私网/回环/链路本地/组播保留段，以及 CGNAT（含云 SLB）。
     * 用于未配 trusted-proxies 时判断是否解析 XFF，以及从 XFF 右侧剥离 SLB 自身地址。
     */
    public static boolean isInfrastructureHop(String ip) {
        return isInternalKeepIp(ip) || isCarrierGradeNatIp(ip);
    }

    /**
     * RFC 6598 共享地址空间 {@code 100.64.0.0/10}（CGNAT）。
     * 阿里云 SLB/ALB 对后端常见 {@code 100.125.x.x} 等，每次连接可能变化，不得当作用户 IP。
     */
    public static boolean isCarrierGradeNatIp(String ip) {
        if (ip == null) {
            return false;
        }
        ip = ip.trim();
        if (!ip.startsWith("100.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 64 && second <= 127;
        } catch (Exception ignored) {
            return false;
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
        // CGNAT / 云 SLB（100.64/10）；与 isCarrierGradeNatIp 一致，避免防火墙把 SLB 当访客
        if (isCarrierGradeNatIp(ip)) return true;
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
