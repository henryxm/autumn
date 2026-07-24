package cn.org.autumn.node;

/**
 * 可插拔节点指纹采集；默认实现委托 {@link Fingerprint#collect()}。
 */
public interface FingerprintProvider {

    Fingerprint.Snapshot collect();

    default String generate() {
        Fingerprint.Snapshot snap = collect();
        return snap != null ? snap.hash32() : null;
    }
}
