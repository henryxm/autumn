package cn.org.autumn.node;

import org.springframework.stereotype.Component;

/**
 * 默认指纹采集：委托 {@link Fingerprint}。
 */
@Component
public class DefaultFingerprintProvider implements FingerprintProvider {

    @Override
    public Fingerprint.Snapshot collect() {
        return Fingerprint.collect();
    }
}
