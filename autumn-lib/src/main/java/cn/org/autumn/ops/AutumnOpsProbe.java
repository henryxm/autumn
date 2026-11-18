package cn.org.autumn.ops;

/**
 * Autumn 运维证书通道探测约定（公开常量，类似 {@code UsingFactory.key}）。
 * 请求头：{@code X-Autumn-Ops-Probe: autumn-ops-probe}。
 */
public final class AutumnOpsProbe {

    public static final String HEADER = "X-Autumn-Ops-Probe";

    /** 固定约定探测 Token（文档公开；仅用于 probe，不授予写权限）。 */
    public static final String TOKEN = "autumn-ops-probe";

    public static final String SESSION_HEADER = "X-Ops-Session";

    private AutumnOpsProbe() {
    }
}
