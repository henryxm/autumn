package cn.org.autumn.node;

import cn.org.autumn.config.Config;
import cn.org.autumn.utils.Uuid;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 系统指纹采集：MAC / machine-id / hostname / os/arch / 可选 salt，以及建议通告主机。
 * <p>
 * {@link #collect()} 返回完整 {@link Snapshot}；{@link #generate()} 取 {@code hash32}（失败则随机 uuid）。
 */
@Slf4j
public final class Fingerprint {

    private Fingerprint() {
    }

    /**
     * 指纹快照（不可变语义：构造后字段不再改）。
     *
     * @param macAddresses           物理网卡 MAC（已过滤常见虚拟网卡）
     * @param machineId              OS machine-id（若可读）
     * @param hostname               主机名
     * @param os                     {@code os.name}
     * @param arch                   {@code os.arch}
     * @param hash32                 SHA-256 前 32 hex，用作节点 uuid 材料
     * @param advertiseHostCandidate 建议对外通告的 IPv4（可能为空）
     */
    public record Snapshot(
            List<String> macAddresses,
            String machineId,
            String hostname,
            String os,
            String arch,
            String hash32,
            String advertiseHostCandidate) {
        public Snapshot {
            macAddresses = macAddresses == null ? List.of() : List.copyOf(macAddresses);
            machineId = StringUtils.defaultString(machineId);
            hostname = StringUtils.defaultString(hostname);
            os = StringUtils.defaultString(os);
            arch = StringUtils.defaultString(arch);
            hash32 = StringUtils.defaultString(hash32);
            advertiseHostCandidate = StringUtils.defaultString(advertiseHostCandidate);
        }
    }

    /** 采集快照；哈希失败时 {@code hash32} 为随机 uuid。 */
    public static Snapshot collect() {
        List<String> macs = collectMacAddresses();
        String machineId = readMachineId();
        String hostname = resolveHostname();
        String os = System.getProperty("os.name", "");
        String arch = System.getProperty("os.arch", "");
        String advertise = pickAdvertiseHostCandidate();
        String hash32;
        try {
            String material = normalizeMaterial(macs, machineId, hostname, os, arch);
            hash32 = sha256Prefix32(material);
        } catch (Exception e) {
            log.warn("Fingerprint collect hash failed, fallback random uuid: {}", e.toString());
            hash32 = Uuid.uuid();
        }
        return new Snapshot(macs, machineId, hostname, os, arch, hash32, advertise);
    }

    /** 等价于 {@code collect().hash32()}。 */
    public static String generate() {
        return collect().hash32();
    }

    /** MAC 顺序无关：排序后拼接；含 machine-id / host / os / arch / 可选 salt。 */
    static String normalizeMaterial(List<String> macs, String machineId, String hostname, String os, String arch) {
        StringBuilder sb = new StringBuilder();
        List<String> sorted = new ArrayList<>(macs != null ? macs : List.of());
        Collections.sort(sorted);
        for (String m : sorted) {
            if (StringUtils.isNotBlank(m)) {
                sb.append("mac:").append(m.trim().toLowerCase(Locale.ROOT)).append('|');
            }
        }
        if (StringUtils.isNotBlank(machineId)) {
            sb.append("mid:").append(machineId.trim().toLowerCase(Locale.ROOT)).append('|');
        }
        if (StringUtils.isNotBlank(hostname)) {
            sb.append("host:").append(hostname.trim().toLowerCase(Locale.ROOT)).append('|');
        }
        if (StringUtils.isNotBlank(os)) {
            sb.append("os:").append(os.trim()).append('|');
        }
        if (StringUtils.isNotBlank(arch)) {
            sb.append("arch:").append(arch.trim()).append('|');
        }
        String salt = Config.getEnv("autumn.node.salt");
        if (StringUtils.isNotBlank(salt)) {
            sb.append("salt:").append(salt.trim()).append('|');
        }
        String material = sb.toString();
        return StringUtils.isNotBlank(material) ? material : "empty-host";
    }

    static List<String> collectMacAddresses() {
        Set<String> out = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) {
                return List.of();
            }
            while (ifaces.hasMoreElements()) {
                NetworkInterface nif = ifaces.nextElement();
                if (nif == null || nif.isLoopback() || !nif.isUp()) {
                    continue;
                }
                String name = StringUtils.defaultString(nif.getName()).toLowerCase(Locale.ROOT);
                String display = StringUtils.defaultString(nif.getDisplayName()).toLowerCase(Locale.ROOT);
                if (name.startsWith("docker") || name.startsWith("br-") || name.startsWith("veth") || name.equals("docker0")
                        || display.contains("virtual") || display.contains("docker")) {
                    continue;
                }
                byte[] hw = nif.getHardwareAddress();
                if (hw == null || hw.length == 0) {
                    continue;
                }
                out.add(formatMac(hw));
            }
        } catch (Exception e) {
            log.debug("collectMacAddresses: {}", e.toString());
        }
        return new ArrayList<>(out);
    }

    static String readMachineId() {
        for (String p : List.of("/etc/machine-id", "/var/lib/dbus/machine-id")) {
            try {
                Path path = Path.of(p);
                if (Files.isRegularFile(path)) {
                    String s = Files.readString(path, StandardCharsets.UTF_8).trim();
                    if (StringUtils.isNotBlank(s)) {
                        return s;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return "";
    }

    static String resolveHostname() {
        String h = firstNonBlank(System.getenv("HOSTNAME"), System.getenv("POD_NAME"), System.getProperty("HOSTNAME"));
        if (h != null) {
            return h;
        }
        try {
            return StringUtils.defaultString(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            return "";
        }
    }

    static String pickAdvertiseHostCandidate() {
        List<String> candidates = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) {
                return "";
            }
            while (ifaces.hasMoreElements()) {
                NetworkInterface nif = ifaces.nextElement();
                if (nif == null || nif.isLoopback() || !nif.isUp()) {
                    continue;
                }
                String name = StringUtils.defaultString(nif.getName()).toLowerCase(Locale.ROOT);
                if (name.startsWith("docker") || name.startsWith("br-") || name.startsWith("veth") || name.equals("docker0")) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                        candidates.add(a.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return candidates.isEmpty() ? "" : candidates.get(0);
    }

    static String sha256Prefix32(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.substring(0, Uuid.LENGTH);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String formatMac(byte[] hw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hw.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format(Locale.ROOT, "%02x", hw[i]));
        }
        return sb.toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v.trim();
            }
        }
        return null;
    }
}
