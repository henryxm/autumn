package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

@ConfigParam(paramKey = DistributedLockConfig.CONFIG_KEY, category = DistributedLockConfig.config, name = "分布式锁配置", description = "配置分布式锁开关、等待时长、租约时长和锁前缀")
public class DistributedLockConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String config = "distributed_lock_config";
    public static final String CONFIG_KEY = "DISTRIBUTED_LOCK_CONFIG";

    @ConfigField(category = InputType.BooleanType, name = "启用分布式能力", description = "是否启用分布式能力，关闭后自动走本地执行")
    private boolean enabled = true;

    @ConfigField(category = InputType.BooleanType, name = "启用Redisson锁", description = "是否启用Redisson分布式锁")
    private boolean enableRedisson = true;

    @ConfigField(category = InputType.LongType, name = "默认等待时长（毫秒）", description = "获取分布式锁时默认等待时长，单位毫秒")
    private long waitMs = 100L;

    @ConfigField(category = InputType.LongType, name = "默认租约时长（毫秒）", description = "分布式锁默认租约时长，单位毫秒")
    private long leaseMs = 30000L;

    @ConfigField(category = InputType.StringType, name = "锁键前缀", description = "分布式锁统一键前缀")
    private String keyPrefix = "autumn:lock:";

    @ConfigField(category = InputType.BooleanType, name = "锁竞争失败时是否降级", description = "当锁获取失败时是否降级执行本地逻辑，默认false（严格模式）")
    private boolean degradeOnAcquireFailure = false;

    @ConfigField(category = InputType.NumberType, name = "锁竞争重试次数", description = "锁竞争失败后的重试次数，默认0（不重试）")
    private int retryTimes = 0;

    @ConfigField(category = InputType.LongType, name = "重试最小退避（毫秒）", description = "锁竞争重试时最小退避时长，默认30毫秒")
    private long retryBackoffMinMs = 30L;

    @ConfigField(category = InputType.LongType, name = "重试最大退避（毫秒）", description = "锁竞争重试时最大退避时长，默认120毫秒")
    private long retryBackoffMaxMs = 120L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableRedisson() {
        return enableRedisson;
    }

    public void setEnableRedisson(boolean enableRedisson) {
        this.enableRedisson = enableRedisson;
    }

    public long getWaitMs() {
        return waitMs;
    }

    public void setWaitMs(long waitMs) {
        this.waitMs = waitMs;
    }

    public long getLeaseMs() {
        return leaseMs;
    }

    public void setLeaseMs(long leaseMs) {
        this.leaseMs = leaseMs;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isDegradeOnAcquireFailure() {
        return degradeOnAcquireFailure;
    }

    public void setDegradeOnAcquireFailure(boolean degradeOnAcquireFailure) {
        this.degradeOnAcquireFailure = degradeOnAcquireFailure;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public long getRetryBackoffMinMs() {
        return retryBackoffMinMs;
    }

    public void setRetryBackoffMinMs(long retryBackoffMinMs) {
        this.retryBackoffMinMs = retryBackoffMinMs;
    }

    public long getRetryBackoffMaxMs() {
        return retryBackoffMaxMs;
    }

    public void setRetryBackoffMaxMs(long retryBackoffMaxMs) {
        this.retryBackoffMaxMs = retryBackoffMaxMs;
    }

    public void normalize() {
        if (waitMs < 0) {
            waitMs = 100L;
        }
        if (leaseMs < 1000L) {
            leaseMs = 30000L;
        }
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = "autumn:lock:";
        } else {
            keyPrefix = keyPrefix.trim();
        }
        if (retryTimes < 0) {
            retryTimes = 0;
        }
        if (retryBackoffMinMs < 0) {
            retryBackoffMinMs = 30L;
        }
        if (retryBackoffMaxMs < retryBackoffMinMs) {
            retryBackoffMaxMs = retryBackoffMinMs;
        }
    }
}
